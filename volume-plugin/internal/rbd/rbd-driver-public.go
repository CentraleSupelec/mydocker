package dockerVolumeRbd

import (
	"github.com/sirupsen/logrus"
	"github.com/ceph/go-ceph/rados"
	"github.com/ceph/go-ceph/rbd"
	"gitlab-research.centralesupelec.fr/mydockervolume/internal/rbd/try"
	"sync"
	"path/filepath"
	"fmt"
	"os/exec"
	"time"
	"regexp"
	"os"
)

type rbdDriver struct {
	root  string            // scratch dir for mounts for this plugin
	conf  map[string]string // ceph config params

	sync.RWMutex            // mutex to guard operations that change volume maps or use conn

	conn  *rados.Conn       // create a connection for each API operation
	ioctx *rados.IOContext  // context for requested pool
}

var (
    rbdHasNoWatchersRegexp = regexp.MustCompile(`^Watchers: none$`)
	rbdBusyRegexp = regexp.MustCompile(`ret=-16$`)
)


// newDriver factory
// builds the driver struct,
// and sets config and
func NewDriver() (error, *rbdDriver) {
	logrus.Debugf("volume-rbd Message=launching rbd driver")

	driver := &rbdDriver{
		root: filepath.Join("/mnt", "volumes"),
		conf: make(map[string]string),
	}

	driver.configure()

	return nil, driver
}


// connect builds up the ceph connection
func (d *rbdDriver) Connect() error {
	logrus.Debugf("volume-rbd Message=connect to ceph pool(%s)", d.conf["pool"])

	// create the go-ceph Client Connection
	var cephConn *rados.Conn
	var err error

	if d.conf["cluster"] == "" {
		cephConn, err = rados.NewConnWithUser(d.conf["keyring_user"])
	} else {
		cephConn, err = rados.NewConnWithClusterAndUser(d.conf["cluster"], d.conf["keyring_user"])
	}
	if err != nil {
		return fmt.Errorf("unable to create ceph connection to cluster(%s) with user(%s): %s", d.conf["cluster"], d.conf["keyring_user"], err)
	}

	err = cephConn.ReadDefaultConfigFile()
	if err != nil {
		return fmt.Errorf("unable to read default config /etc/ceph/ceph.conf: %s", err)
	}

	err = cephConn.Connect()
	if err != nil {
		return fmt.Errorf("unable to open the ceph cluster connection: %s", err)
	}

	// can now set conn in driver
	d.conn = cephConn

	// setup the requested pool context
	ioctx, err := d.conn.OpenIOContext(d.conf["pool"])
	if err != nil {
		return fmt.Errorf("unable to open context(%s): %s", d.conf["pool"], err)
	}

	d.ioctx = ioctx
	// Setup the namespace for the volumes we are working on
	// default is ""  as it is the name 'internal' name for image without namespace
	// Calling ioctx.SetNamespace("") is basically a noop
	ioctx.SetNamespace(d.conf["namespace"])

	return nil
}


func (d *rbdDriver) Shutdown() {
	logrus.Debugf("volume-rbd Message=connection shutdown from ceph")

	if d.ioctx != nil {
		d.ioctx.Destroy()
	}
	if d.conn != nil {
		d.conn.Shutdown()
	}
}


func (d *rbdDriver) RbdImageExists(imageName string) (error, bool) {
	logrus.Debugf("volume-rbd Name=%s Message=checking if exists rbd image in pool(%s)", imageName, d.conf["pool"])

	if imageName == "" {
		return fmt.Errorf("error checking empty imageName in pool(%s)", d.conf["pool"]), false
	}

	img := rbd.GetImage(d.ioctx, imageName)
	err := img.Open(true)
	defer img.Close()

	if err != nil {
		if err == rbd.RbdErrorNotFound {
			return nil, false
		}
		return err, false
	}
	return nil, true
}


func (d *rbdDriver) GetRbdImages() (err error, imageNames []string) {

	imageNames, err = rbd.GetImageNames(d.ioctx)

    return err, imageNames
}


func (d *rbdDriver) CreateRbdImage(imageName string, size uint64, order int, fstype string, mkfsOptions string) error {
	logrus.Debugf("volume-rbd Name=%s Message=create image in pool(%s) with size(%dMB) and fstype(%s)", imageName, d.conf["pool"], size, fstype)

	// check that fs is valid type (needs mkfs.fstype in PATH)
	mkfs, err := exec.LookPath("mkfs." + fstype)
	if err != nil {
		return fmt.Errorf("unable to find mkfs.(%s): %s", fstype, err)
	}


	// create the image
	sizeInBytes := size * 1024 * 1024
	_, err = rbd.Create(d.ioctx, imageName, sizeInBytes, order)
	if err != nil {
		return err
	}


	// map to kernel to let initialize fs
	err = d.mapImage(imageName)
	if err != nil {
		// Same reasoning as the other two failure points: Create is about to return an error, so
		// dockerd will never record this volume and nothing downstream can find the image. A
		// deferred removeRbdImage discarded its own failure and left it orphaned silently.
		d.reapImageAfterFailedCreate(imageName, err)
		return err
	}

	// make the filesystem (give it some time)
	device := d.getTheDevice(imageName)
	// skip discard/TRIM at format time: it dominates format cost on large
	// images and pushed creates past the request deadline
	var mkfsArgs []string
	switch fstype {
	case "ext2", "ext3", "ext4":
		mkfsArgs = append(mkfsArgs, "-E", "nodiscard")
	case "xfs":
		mkfsArgs = append(mkfsArgs, "-K")
	}
	if mkfsOptions != "" {
		mkfsArgs = append(mkfsArgs, mkfsOptions)
	}
	mkfsArgs = append(mkfsArgs, device)
	// retry is safe here: the image is virgin, a partially written filesystem
	// from a killed attempt is simply reformatted
	for attempt := 0; attempt <= shellRetries(); attempt++ {
		if attempt > 0 {
			logrus.Warnf("volume-rbd Name=%s Message=mkfs: retry %d after: %s", imageName, attempt, err)
		}
		_, err = shWithDefaultTimeout(mkfs, mkfsArgs...)
		if err == nil {
			break
		}
	}
	if err != nil {
		// mkfs is the failure being reported; an unmap failure on top of it is logged rather than
		// substituted, but it must not be silent - it means a device is left mapped.
		if unmapErr := d.unmapImage(imageName); unmapErr != nil {
			logrus.Errorf("volume-rbd Name=%s Message=unmap after failed mkfs: %s", imageName, unmapErr)
		}
		// Checked, not deferred-and-discarded: if this removal fails the image is orphaned, and
		// that has to be said out loud rather than dropped on the floor.
		d.reapImageAfterFailedCreate(imageName, err)
		return err
	}

	// Leave the image unmapped. Called explicitly, not deferred: a deferred call discards its
	// error, which let Create report success with the device still mapped - the volume then looks
	// healthy while its teardown never completed.
	if err := d.unmapImage(imageName); err != nil {
		// Returning an error here means dockerd never records the volume, so from this moment on
		// nothing downstream can find the image: Docker cannot remove a volume it does not know
		// about, docker-api's cleanup inspects by volume name and gets not-found, and the unmap
		// watchdog only observes release. This function is the last place that still knows the
		// name, so reclaiming it is its job.
		d.reapImageAfterFailedCreate(imageName, err)
		return fmt.Errorf("unable to unmap %s after create: %s", imageName, err)
	}

	return nil
}

// reapImageAfterFailedCreate removes the image behind a Create that is about to fail.
//
// If the removal also fails - most likely because the device is still mapped, which is the very
// reason we are here - the image is genuinely orphaned in the pool with no owner anywhere in the
// stack. That case is logged as RBD_ORPHAN_IMAGE with the full spec so it can be reclaimed by hand
// or by a sweep, because an unreclaimable image nobody knows about is worse than a noisy log line.
// A durable reaper is the real answer and does not exist yet.
func (d *rbdDriver) reapImageAfterFailedCreate(imageName string, cause error) {
	if rmErr := d.RemoveRbdImageWithRetries(imageName); rmErr != nil {
		logrus.Errorf(
			"volume-rbd Name=%s Message=RBD_ORPHAN_IMAGE pool=%s namespace=%s image=%s abandoned after failed create (%s), removal also failed: %s",
			imageName, d.conf["pool"], d.conf["namespace"], imageName, cause, rmErr,
		)
		return
	}
	logrus.Warnf("volume-rbd Name=%s Message=removed image after failed create: %s", imageName, cause)
}


func (d *rbdDriver) RemoveRbdImageWithRetries(imageName string) error {

	err := try.Do(func(attempt int) (bool, error) {
		var err error
		err = d.removeRbdImage(imageName)

		if err != nil && rbdBusyRegexp.MatchString(err.Error()) {
			const MAX_ATTEMPTS = 3;
			time.Sleep(2 * time.Second)

			return attempt < MAX_ATTEMPTS, err
		}

		return false, err
	})

	return err
}


func (d *rbdDriver) MountRbdImage(imageName string) (err error, mountpoint string) {
	logrus.Debugf("volume-rbd Name=%s Message=MountRbdImage map and mount", imageName)


    err = d.errIfRbdImageHasWatchers(imageName)
	if err != nil {
        logrus.Warnf("volume-rbd Name=%s Message=MountRbdImage image has watchers: %s", imageName, err)
	}



	err = d.mapImage(imageName)
	if err != nil {
		return fmt.Errorf("unable to map %s: %s", imageName, err), ""
	}


	// check for mountdir - create if necessary
	mountpoint = d.GetMountPointPath(imageName)
	err = os.MkdirAll(mountpoint, os.ModeDir | os.FileMode(int(0775)))
	if err != nil {
		// Rollback is best-effort and the mkdir failure is what the caller needs, but a failed
		// rollback leaves a mapped device and must not vanish into a discarded deferred return.
		if freeErr := d.FreeUpRbdImage(imageName); freeErr != nil {
			logrus.Errorf("volume-rbd Name=%s Message=rollback after mountpoint failure left state behind: %s", imageName, freeErr)
		}
		return fmt.Errorf("unable to make mountpoint %s: %s", mountpoint, err), ""
	}


	// mount
	mountOptions := os.Getenv("MOUNT_OPTIONS")
	err = d.mountImage(imageName, mountOptions)
	if err != nil {
		if freeErr := d.FreeUpRbdImage(imageName); freeErr != nil {
			logrus.Errorf("volume-rbd Name=%s Message=rollback after mount failure left state behind: %s", imageName, freeErr)
		}
		return fmt.Errorf("unable to mount: %s", err), ""
	}

	return err, mountpoint

}

/**
 * Freeing Up an RBD image means
 * unmount + unmap and remove mountpoint
 *
 * Idempotent, but not silent: a state that is already clean is success, while a step that
 * genuinely failed is reported. Returning nil unconditionally made Unmount and Remove answer
 * success with the device still mapped and the image still in use, which is indistinguishable
 * from a real teardown to every caller and to the operator.
 */
func (d *rbdDriver) FreeUpRbdImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=free up image", imageName)

	mountpoint := d.GetMountPointPath(imageName)

	// Already unmounted is success; a failure to unmount something still mounted is not.
	if isMountpoint(mountpoint) {
		if err := d.unmountDevice(imageName); err != nil {
			logrus.Errorf("volume-rbd Name=%s Message=unable to unmount: %s", imageName, err)
			return fmt.Errorf("unable to unmount %s: %s", mountpoint, err)
		}
	} else {
		logrus.Debugf("volume-rbd Name=%s Message=not mounted, nothing to unmount", imageName)
	}

	// unmapImage already treats "not mapped" as success and only errors when a release could not
	// be confirmed, so its error is exactly the case worth propagating.
	if err := d.unmapImage(imageName); err != nil {
		logrus.Errorf("volume-rbd Name=%s Message=unable to unmap: %s", imageName, err)
		return fmt.Errorf("unable to unmap %s: %s", imageName, err)
	}

	// A missing mountpoint is the desired end state, so only a real removal failure counts.
	if err := os.Remove(mountpoint); err != nil && !os.IsNotExist(err) {
		logrus.Errorf("volume-rbd Name=%s Message=unable to remove mountpoint(%s): %s", imageName, mountpoint, err)
		return fmt.Errorf("unable to remove mountpoint %s: %s", mountpoint, err)
	}

	return nil
}



// returns the expected path inside plugin container. The named "propagated mount"
func (d *rbdDriver) GetMountPointPath(imageName string) string {
	return filepath.Join(d.root, imageName)
}