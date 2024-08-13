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

func NewFSDriver(providedRoot string) (error, *MydockerFsDriver) {
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
	driver := &MydockerFsDriver{
		root: root,
	}
	return nil, driver
}

type MydockerFsDriver struct {
	d    volume.Driver
	root string
}

func (d *MydockerFsDriver) Create(request *volume.CreateRequest) error {
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

func (d *MydockerFsDriver) List() (*volume.ListResponse, error) {
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

func (d *MydockerFsDriver) Get(request *volume.GetRequest) (*volume.GetResponse, error) {
	fullPath := path.Join(d.root, request.Name)
	if _, err := os.Stat(fullPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("volume %s does not exist", request.Name)
	}
	return &volume.GetResponse{Volume: &volume.Volume{
		Name:       request.Name,
		Mountpoint: fullPath,
	}}, nil
}

func (d *MydockerFsDriver) Remove(request *volume.RemoveRequest) error {
	fullPath := path.Join(d.root, request.Name)
	log.Debugf("volume-mydocker Message=Removing %s", fullPath)
	err := os.RemoveAll(fullPath)
	if err != nil {
		return err
	}
	return nil
}

func (d *MydockerFsDriver) Path(request *volume.PathRequest) (*volume.PathResponse, error) {
	fullPath := path.Join(d.root, request.Name)
	if _, err := os.Stat(fullPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("volume %s does not exist", request.Name)
	}
	return &volume.PathResponse{
		Mountpoint: fullPath,
	}, nil
}

func (d *MydockerFsDriver) Mount(request *volume.MountRequest) (*volume.MountResponse, error) {
	fullPath := path.Join(d.root, request.Name)
	if _, err := os.Stat(fullPath); os.IsNotExist(err) {
		return nil, fmt.Errorf("volume %s does not exist", request.Name)
	}
	return &volume.MountResponse{
		Mountpoint: fullPath,
	}, nil
}

func (d *MydockerFsDriver) Unmount(request *volume.UnmountRequest) error {
	return nil
}

func (d *MydockerFsDriver) Capabilities() *volume.CapabilitiesResponse {
	return &volume.CapabilitiesResponse{
		Capabilities: volume.Capability{
			Scope: "global",
		},
	}
}
