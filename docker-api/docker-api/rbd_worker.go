package main

import (
	"fmt"
	"github.com/ceph/go-ceph/rados"
	"github.com/ceph/go-ceph/rbd"
	"github.com/sirupsen/logrus"
	"os/exec"
	"path/filepath"
	"regexp"
	"time"
)

var rbdUnmapBusyRegexp = regexp.MustCompile(`^exit status 16$`)

type rbdDriver struct {
	conn  *rados.Conn
	ioctx *rados.IOContext
}

func (d *rbdDriver) Create(imageName string, size uint32) error {
	err := d.connect()
	if err != nil {
		return err
	}
	err, exist := d.rbdImageExists(imageName)
	if err != nil {
		return err
	}
	if exist {
		return nil
	}
	err = d.createRbdImage(imageName, size)
	if err != nil {
		return err
	}
	d.Shutdown()
	return nil
}

func (d *rbdDriver) connect() error {
	logrus.Debugf("volume-rbd Message=connect to ceph pool(%s)", c.CephPool)

	cephConn, err := rados.NewConnWithClusterAndUser(c.CephCluster, c.KeyringUser)

	if err != nil {
		return fmt.Errorf("unable to create ceph connection to cluster(%s) with user(%s): %s", c.CephCluster, c.KeyringUser, err)
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
	ioctx, err := d.conn.OpenIOContext(c.CephPool)
	if err != nil {
		return fmt.Errorf("unable to open context(%s): %s", c.CephPool, err)
	}

	d.ioctx = ioctx

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

func (d *rbdDriver) rbdImageExists(imageName string) (error, bool) {
	logrus.Debugf("volume-rbd Name=%s Message=checking if exists rbd image in pool(%s)", imageName, c.CephPool)

	if imageName == "" {
		return fmt.Errorf("error checking empty imageName in pool(%s)", c.CephPool), false
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

func (d *rbdDriver) createRbdImage(imageName string, size uint32) error {
	fstype := "ext4"
	mkfsOptions := "-O ^mmp"
	order := 22 // 4KB Objects
	logrus.Debugf("volume-rbd Name=%s Message=create image in pool(%s) with size(%dMB) and fstype(%s)", imageName, c.CephPool, size, fstype)

	// check that fs is valid type (needs mkfs.fstype in PATH)
	mkfs, err := exec.LookPath("mkfs." + fstype)
	if err != nil {
		return fmt.Errorf("unable to find mkfs.(%s): %s", fstype, err)
	}


	// create the image
	var sizeInBytes uint64
	sizeInBytes = uint64(size) * 1024 * 1024
	_, err = rbd.Create(d.ioctx, imageName, sizeInBytes, order)
	if err != nil {
		return err
	}


	// map to kernel to let initialize fs
	err = d.mapImage(imageName)
	if err != nil {
		defer d.removeRbdImage(imageName)
		return err
	}

	// make the filesystem (give it some time)
	device := d.getTheDevice(imageName)
	_, err = shWithTimeout(5 * time.Minute, mkfs, mkfsOptions, device)
	if err != nil {
		_ = d.unmapImage(imageName)
		defer d.removeRbdImage(imageName)
		return err
	}


	// leave the image unmaped
	defer d.unmapImage(imageName)

	return nil
}

func (d *rbdDriver) mapImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=rbd map", imageName)

	_, err := d.rbdsh("map", imageName)

	return err
}


func (d *rbdDriver) unmapImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=rbd unmap", imageName)

	_, err := d.rbdsh("unmap", imageName)

	if err != nil {
		// NOTE: rbd unmap exits 16 if device is still being used - unlike umount.  try to recover differently in that case
		if rbdUnmapBusyRegexp.MatchString(err.Error()) {
			return err
		}

		logrus.Errorf("volume-rbd Name=%s Message=rbd unmap: %s", imageName, err.Error())
		// other error, continue and fail safe
	}

	return nil
}


func (d *rbdDriver) removeRbdImage(imageName string) error {
	logrus.Debugf("volume-rbd Name=%s Message=remove rbd image", imageName)

	rbdImage := rbd.GetImage(d.ioctx, imageName)

	return rbdImage.Remove()
}


// rbdsh will call rbd with the given command arguments, also adding config, user and pool flags
func (d *rbdDriver) rbdsh(command string, args ...string) (string, error) {

	args = append([]string{"--cluster", c.CephCluster, "--pool", c.CephPool, "--name", c.KeyringUser, command}, args...)

	return shWithDefaultTimeout("rbd", args...)
}


// returns the aliased device under device_map_root
func (d *rbdDriver) getTheDevice(imageName string) string {
	return filepath.Join("/dev/rbd", c.CephPool, imageName)
}

type CreateRbdImageWorkerResponse struct {
	err 		error
	imageName 	string
	workerIndex int
}

type CreateRbdImageWorkerRequest struct {
	imageName 	string
	size 		uint32
	workerIndex int
}


func CreateRbdImageWorker(in <-chan CreateRbdImageWorkerRequest, out chan<- CreateRbdImageWorkerResponse) {
	driver := &rbdDriver{}
	for request := range in {
		err := driver.Create(request.imageName, request.size)
		out <- CreateRbdImageWorkerResponse{imageName: request.imageName, err: err, workerIndex: request.workerIndex}
	}
}
