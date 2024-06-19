module gitlab-research.centralesupelec.fr/mydockervolume

go 1.22

require (
	github.com/docker/go-plugins-helpers v0.0.0-20211224144127-6eecb7beb651
	github.com/sirupsen/logrus v1.9.3
	github.com/wetopi/docker-volume-rbd v0.0.0-20220511082147-6481d0b09eb9
)

require (
	github.com/Microsoft/go-winio v0.6.1 // indirect
	github.com/ceph/go-ceph v0.26.0 // indirect
	github.com/coreos/go-systemd v0.0.0-20191104093116-d3cd4ed1dbcf // indirect
	github.com/docker/go-connections v0.5.0 // indirect
	golang.org/x/mod v0.15.0 // indirect
	golang.org/x/sys v0.17.0 // indirect
	golang.org/x/tools v0.18.0 // indirect
)

replace github.com/wetopi/docker-volume-rbd v0.0.0-20220511082147-6481d0b09eb9 => github.com/pdesgarets/docker-volume-rbd v0.0.0-20240619160931-588d3b276faa
