package mydockervolume

import (
	"errors"
	"fmt"
	"github.com/docker/go-plugins-helpers/volume"
	log "github.com/sirupsen/logrus"
	"io/ioutil"
	"os"
	"path"
	"strings"
)

func NewFSDriver(providedRoot string) (error, *mydockerFsDriver) {
	var root string
	if providedRoot == "" {
		root = "/mnt/volumes"
	} else {
		root = providedRoot
	}
	log.Debugf("volume-mydocker Message=Creating %s", root)
	err := os.MkdirAll(root, os.FileMode(0700))
	if err != nil {
		return err, nil
	}
	driver := &mydockerFsDriver{
		root: root,
	}
	return nil, driver
}

type mydockerFsDriver struct {
	d    volume.Driver
	root string
}

func (d *mydockerFsDriver) Create(request *volume.CreateRequest) error {
	if strings.Contains(request.Name, "/") {
		return errors.New("name is invalid because it contains '/'")
	}
	fullPath := path.Join(d.root, request.Name)
	log.Debugf("volume-mydocker Message=Creating %s", fullPath)
	if err := os.MkdirAll(fullPath, os.ModeDir|os.FileMode(0775)); err != nil {
		return err
	}
	return nil
}

func (d *mydockerFsDriver) List() (*volume.ListResponse, error) {
	fileInfos, err := ioutil.ReadDir(d.root)
	if err != nil {
		return nil, err
	}
	var volumes []*volume.Volume
	for _, fileInfo := range fileInfos {
		volumes = append(volumes, &volume.Volume{
			Name:       fileInfo.Name(),
			Mountpoint: path.Join(d.root, fileInfo.Name()),
		})
	}
	return &volume.ListResponse{Volumes: volumes}, nil
}

func (d *mydockerFsDriver) Get(request *volume.GetRequest) (*volume.GetResponse, error) {
	fullPath := path.Join(d.root, request.Name)
	if _, err := os.Stat(fullPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("volume %s does not exist", request.Name)
	}
	return &volume.GetResponse{Volume: &volume.Volume{
		Name:       request.Name,
		Mountpoint: fullPath,
	}}, nil
}

func (d *mydockerFsDriver) Remove(request *volume.RemoveRequest) error {
	fullPath := path.Join(d.root, request.Name)
	log.Debugf("volume-mydocker Message=Removing %s", fullPath)
	err := os.RemoveAll(fullPath)
	if err != nil {
		return err
	}
	return nil
}

func (d *mydockerFsDriver) Path(request *volume.PathRequest) (*volume.PathResponse, error) {
	fullPath := path.Join(d.root, request.Name)
	if _, err := os.Stat(fullPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("volume %s does not exist", request.Name)
	}
	return &volume.PathResponse{
		Mountpoint: fullPath,
	}, nil
}

func (d *mydockerFsDriver) Mount(request *volume.MountRequest) (*volume.MountResponse, error) {
	fullPath := path.Join(d.root, request.Name)
	if _, err := os.Stat(fullPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("volume %s does not exist", request.Name)
	}
	return &volume.MountResponse{
		Mountpoint: fullPath,
	}, nil
}

func (d *mydockerFsDriver) Unmount(request *volume.UnmountRequest) error {
	return nil
}

func (d *mydockerFsDriver) Capabilities() *volume.CapabilitiesResponse {
	return &volume.CapabilitiesResponse{
		Capabilities: volume.Capability{
			Scope: "global",
		},
	}
}
