package dockerVolumeRbd

import (
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/sirupsen/logrus"
)

// The rbd CLI's unmap waits for a udev confirmation over a libudev netlink
// monitor. On affected hosts that wait never wakes even though the kernel
// removed the device in well under a second, so every unmap used to stall
// until the shell timeout. We do not depend on that confirmation at all:
// the /sys/bus/rbd/devices/<id> entry disappears exactly when the kernel
// finishes releasing the image, which is the signal libudev was supposed
// to carry. The CLI runs in the background and we watch sysfs instead.
const rbdSysfsDevicesDir = "/sys/bus/rbd/devices"

// Sync wait stays far under dockerd's 120s plugin proxy deadline; the kernel
// side normally completes in ~300ms. Past the sync budget a watchdog keeps
// watching up to the hard cap and logs the outcome.
const (
	unmapSyncWait     = 30 * time.Second
	unmapWatchdogWait = 5 * time.Minute
	unmapPollInterval = 100 * time.Millisecond
)

func readSysfsAttr(id string, attr string) string {
	b, err := os.ReadFile(filepath.Join(rbdSysfsDevicesDir, id, attr))
	if err != nil {
		return ""
	}
	return strings.TrimSpace(string(b))
}

// sysfsDeviceIsOurs reports whether device id currently maps our
// pool/namespace/image. A vanished entry or one whose attributes no longer
// match means the id was released (and possibly reused by a concurrent map),
// so the unmap we were waiting for is done.
func (d *rbdDriver) sysfsDeviceIsOurs(id string, imageName string) bool {
	if readSysfsAttr(id, "name") != imageName {
		return false
	}
	if readSysfsAttr(id, "pool") != d.conf["pool"] {
		return false
	}
	// namespace attribute is absent on older kernels; both sides read ""
	return readSysfsAttr(id, "namespace") == d.conf["namespace"]
}

// sysfsMappedDeviceIDs lists the rbd device ids currently mapping imageName.
func (d *rbdDriver) sysfsMappedDeviceIDs(imageName string) []string {
	entries, err := os.ReadDir(rbdSysfsDevicesDir)
	if err != nil {
		logrus.Warnf("volume-rbd Name=%s Message=cannot read %s: %s", imageName, rbdSysfsDevicesDir, err)
		return nil
	}

	var ids []string
	for _, e := range entries {
		if d.sysfsDeviceIsOurs(e.Name(), imageName) {
			ids = append(ids, e.Name())
		}
	}
	return ids
}

func (d *rbdDriver) sysfsAllReleased(ids []string, imageName string) bool {
	for _, id := range ids {
		if d.sysfsDeviceIsOurs(id, imageName) {
			return false
		}
	}
	return true
}

// watchUnmapCompletion keeps watching a slow unmap after the sync budget ran
// out. Grep targets: RBD_UNMAP_LATE (completed after the sync wait) and
// RBD_UNMAP_ABANDONED (still mapped at the hard cap; leaked mapping needs a
// manual `rbd unmap`).
func (d *rbdDriver) watchUnmapCompletion(ids []string, imageName string, started time.Time) {
	deadline := started.Add(unmapWatchdogWait)
	for time.Now().Before(deadline) {
		if d.sysfsAllReleased(ids, imageName) {
			logrus.Warnf("volume-rbd Name=%s Message=RBD_UNMAP_LATE unmap completed after %s", imageName, time.Since(started))
			return
		}
		time.Sleep(500 * time.Millisecond)
	}
	logrus.Errorf("volume-rbd Name=%s Message=RBD_UNMAP_ABANDONED device(s) %v still mapped after %s, giving up; unmap manually", imageName, ids, unmapWatchdogWait)
}
