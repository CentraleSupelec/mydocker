package main

import (
	"github.com/docker/go-plugins-helpers/volume"
	log "github.com/sirupsen/logrus"
	wetopi "github.com/wetopi/docker-volume-rbd/lib"
	"gitlab-research.centralesupelec.fr/mydockervolume/mydockervolume"
	"os"
)

const socketAddress = "/run/docker/plugins/mydockervolume.sock"

func main() {
	providedLogLevel := os.Getenv("LOG_LEVEL")
	level, err := log.ParseLevel(providedLogLevel)
	if err != nil {
		log.Infof("Could not parse log level '%s', using Warning instead", providedLogLevel)
		level = log.WarnLevel
	}

	var mydockerDriver volume.Driver
	switch mode := os.Getenv("DRIVER_MODE"); mode {
	case "FS":
		err, mydockerDriver = mydockervolume.NewFSDriver(os.Getenv("ROOT_DIR"))
		if err != nil {
			log.Fatal(err)
			os.Exit(1)
		}
	case "RBD":
		err, mydockerDriver = wetopi.NewDriver()
	default:
		log.Fatalf("unsupported DRIVER_MODE %s", mode)
		os.Exit(1)
	}

	h := volume.NewHandler(mydockerDriver)

	log.Infof("plugin(mydockervolume) started with log level(%s) attending socket(%s)", level, socketAddress)
	log.SetLevel(level)
	log.Error(h.ServeUnix(socketAddress, 0))
}
