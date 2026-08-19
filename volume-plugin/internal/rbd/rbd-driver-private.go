package dockerVolumeRbd

import (
	"fmt"
	"github.com/ceph/go-ceph/rbd"
	"github.com/sirupsen/logrus"
	"golang.org/x/sys/unix"
	"os"
	"path/filepath"
	"time"
)

func (d *rbdDriver) mapImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=rbd map", imageName)

	_, err := d.rbdsh("map", filepath.Join(d.conf["pool"], d.conf["namespace"], imageName))

	return err
}

func (d *rbdDriver) unmapImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=rbd unmap", imageName)

	ids := d.sysfsMappedDeviceIDs(imageName)
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
		_, err := d.rbdsh("unmap", filepath.Join(d.conf["pool"], d.conf["namespace"], imageName))
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
			if err != nil {
				// NOTE: rbd unmap exits 16 if device is still being used - unlike umount.  try to recover differently in that case
				if rbdUnmapBusyRegexp.MatchString(err.Error()) {
					return err
				}
				logrus.Errorf("volume-rbd Name=%s Message=rbd unmap: %s", imageName, err.Error())
				// other error, continue and fail safe
				return nil
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
	_, err := shWithDefaultTimeout("mount", mountOptions, device, mountpoint)
	if err != nil {
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
