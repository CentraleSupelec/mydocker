module gitlab-research.centralesupelec.fr/mydockervolume

go 1.15

require (
	github.com/Microsoft/go-winio v0.5.2 // indirect
	github.com/docker/go-plugins-helpers v0.0.0-20211224144127-6eecb7beb651
	github.com/sirupsen/logrus v1.8.1
	github.com/wetopi/docker-volume-rbd v0.0.0-20220511082147-6481d0b09eb9
	golang.org/x/net v0.0.0-20220425223048-2871e0cb64e4 // indirect
)

replace github.com/wetopi/docker-volume-rbd v0.0.0-20220511082147-6481d0b09eb9 => github.com/CentraleSupelec/docker-volume-rbd v0.0.0-20220511082147-6481d0b09eb9
