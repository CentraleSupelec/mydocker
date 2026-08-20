package dockerVolumeRbd

import (
	"errors"
	"fmt"
	"github.com/ceph/go-ceph/rbd"
	"github.com/sirupsen/logrus"
	"golang.org/x/sys/unix"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

func (d *rbdDriver) mapImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=rbd map", imageName)

	spec := filepath.Join(d.conf["pool"], d.conf["namespace"], imageName)

	var err error
	for attempt := 0; attempt <= shellRetries(); attempt++ {
		if attempt > 0 {
			// a killed map attempt may have succeeded kernel-side before the
			// CLI reported; re-mapping would create a second device
			if ids, idsErr := d.sysfsMappedDeviceIDs(imageName); idsErr == nil && len(ids) > 0 {
				logrus.Warnf("volume-rbd Name=%s Message=rbd map: device present in sysfs after failed attempt, treating as mapped", imageName)
				return nil
			}
			logrus.Warnf("volume-rbd Name=%s Message=rbd map: retry %d after: %s", imageName, attempt, err)
		}
		_, err = d.rbdsh("map", spec)
		if err == nil {
			return nil
		}
	}

	return err
}

// isMountpoint reports whether path is a mount point in our namespace,
// per /proc/self/mountinfo (field 5 is the mount point).
func isMountpoint(path string) bool {
	data, err := os.ReadFile("/proc/self/mountinfo")
	if err != nil {
		return false
	}
	for _, line := range strings.Split(string(data), "\n") {
		fields := strings.Fields(line)
		if len(fields) >= 5 && fields[4] == path {
			return true
		}
	}
	return false
}

// isUnmapBusy detects rbd unmap's exit 16: device is still being used -
// unlike umount. Callers recover differently in that case.
func isUnmapBusy(err error) bool {
	var ee *exec.ExitError
	return errors.As(err, &ee) && ee.ExitCode() == 16
}

func (d *rbdDriver) unmapImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=rbd unmap", imageName)

	spec := filepath.Join(d.conf["pool"], d.conf["namespace"], imageName)

	ids, err := d.sysfsMappedDeviceIDs(imageName)
	if err != nil {
		// no usable sysfs view: fall back to the plain synchronous CLI unmap
		logrus.Warnf("volume-rbd Name=%s Message=rbd unmap: sysfs unavailable (%s), synchronous CLI fallback", imageName, err)
		_, err := d.rbdsh("unmap", spec)
		if err != nil {
			if isUnmapBusy(err) {
				return err
			}
			logrus.Errorf("volume-rbd Name=%s Message=rbd unmap: %s", imageName, err.Error())
			// other error, continue and fail safe
		}
		return nil
	}
	if len(ids) == 0 {
		logrus.Debugf("volume-rbd Name=%s Message=rbd unmap: not mapped, nothing to do", imageName)
		return nil
	}

	// The CLI runs in the background: its udev confirmation wait can hang
	// forever on affected hosts. Completion is decided by sysfs, not by the
	// CLI returning (see unmap-sysfs.go).
	started := time.Now()
	cliErr := make(chan error, 1)
	go func() {
		_, err := d.rbdsh("unmap", spec)
		cliErr <- err
	}()

	syncDeadline := started.Add(unmapSyncWait)
	for {
		if d.sysfsAllReleased(ids, imageName) {
			logrus.Debugf("volume-rbd Name=%s Message=rbd unmap: released in %s", imageName, time.Since(started))
			return nil
		}

		select {
		case err := <-cliErr:
			cliErr = nil // consumed; a nil channel is never selected again
			if err != nil {
				if isUnmapBusy(err) {
					return err
				}
				logrus.Errorf("volume-rbd Name=%s Message=rbd unmap: %s", imageName, err.Error())
				// keep watching sysfs: the kernel may have released the
				// device despite the CLI failure, and if it did not the
				// watchdog must still get a chance to report it
			}
		case <-time.After(unmapPollInterval):
		}

		if time.Now().After(syncDeadline) {
			logrus.Errorf("volume-rbd Name=%s Message=RBD_UNMAP_SLOW device(s) %v still mapped after %s, continuing fail-safe; watchdog will report", imageName, ids, unmapSyncWait)
			go d.watchUnmapCompletion(ids, imageName, started)
			return nil
		}
	}
}

func (d *rbdDriver) mountImage(imageName string, mountOptions string) error {

	device := d.getTheDevice(imageName)
	mountpoint := d.GetMountPointPath(imageName)

	logrus.Debugf("volume-rbd Name=%s Message=mount %s %s %s", imageName, mountOptions, device, mountpoint)

	// err := unix.Mount(device, mountpoint, "auto", 0, "")
	// note unix.Mount does not work with our aliased device, we user the sh version.
	var err error
	for attempt := 0; attempt <= shellRetries(); attempt++ {
		if attempt > 0 {
			// a killed mount attempt may have completed the syscall;
			// re-mounting would stack a second mount on the target
			if isMountpoint(mountpoint) {
				logrus.Warnf("volume-rbd Name=%s Message=mount: %s already mounted after failed attempt, treating as mounted", imageName, mountpoint)
				break
			}
			logrus.Warnf("volume-rbd Name=%s Message=mount: retry %d after: %s", imageName, attempt, err)
		}
		_, err = shWithDefaultTimeout("mount", mountOptions, device, mountpoint)
		if err == nil {
			break
		}
	}
	if err != nil && !isMountpoint(mountpoint) {
		return err
	}

	volumeMountMode := os.Getenv("VOLUME_MOUNT_MODE")
	if volumeMountMode != "" {
		logrus.Debugf("volume-rbd Name=%s Message=chmod %s to %s %s", imageName, mountpoint, volumeMountMode, device)
		_, err = shWithDefaultTimeout("chmod", volumeMountMode, mountpoint)
	} else {
		logrus.Debugf("volume-rbd Name=%s Message=no chmod for %s", imageName, device)
	}

	volumeMountDefaultACL := os.Getenv("VOLUME_MOUNT_DEFAULT_ACL")
	if volumeMountDefaultACL != "" {
		logrus.Debugf("volume-rbd Name=%s Message=setfacl %s to %s %s", imageName, mountpoint, volumeMountDefaultACL, device)
		_, err = shWithDefaultTimeout("setfacl", "-dm", volumeMountDefaultACL, mountpoint)
	} else {
		logrus.Debugf("volume-rbd Name=%s Message=no ACL for %s", imageName, device)
	}

	return err
}

func (d *rbdDriver) unmountDevice(imageName string) error {

	mountpoint := d.GetMountPointPath(imageName)
	logrus.Debugf("volume-rbd Message=umount %s", mountpoint)

	err := unix.Unmount(mountpoint, 0)

	return err
}

func (d *rbdDriver) errIfRbdImageHasWatchers(imageName string) error {

	status, err := d.rbdsh("status", imageName)
	if err != nil {
		return err
	}

	if rbdHasNoWatchersRegexp.MatchString(status) {
		return nil
	}

	return fmt.Errorf("image with %s", status)
}

func (d *rbdDriver) removeRbdImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=remove rbd image", imageName)

	rbdImage := rbd.GetImage(d.ioctx, imageName)

	return rbdImage.Remove()
}

// rbdsh will call rbd with the given command arguments, also adding config, user and pool flags
func (d *rbdDriver) rbdsh(command string, args ...string) (string, error) {

	args = append([]string{"--cluster", d.conf["cluster"], "--pool", d.conf["pool"], "--name", d.conf["keyring_user"], command}, args...)

	return shWithDefaultTimeout("rbd", args...)
}

// returns the aliased device under device_map_root
func (d *rbdDriver) getTheDevice(imageName string) string {
	return filepath.Join(d.conf["device_map_root"], d.conf["pool"], imageName)
}
