module gitlab-research.centralesupelec.fr/mydockervolume

go 1.22

require (
	github.com/docker/go-plugins-helpers v0.0.0-20211224144127-6eecb7beb651
	github.com/sirupsen/logrus v1.8.1
	github.com/wetopi/docker-volume-rbd v0.0.0-20220511082147-6481d0b09eb9
)

require (
	github.com/Microsoft/go-winio v0.5.2 // indirect
	github.com/ceph/go-ceph v0.15.0 // indirect
	github.com/coreos/go-systemd v0.0.0-20191104093116-d3cd4ed1dbcf // indirect
	github.com/docker/go-connections v0.4.0 // indirect
	golang.org/x/net v0.0.0-20220425223048-2871e0cb64e4 // indirect
	golang.org/x/sys v0.0.0-20211216021012-1d35b9e2eb4e // indirect
)
replace (
	github.com/wetopi/docker-volume-rbd v0.0.0-20220511082147-6481d0b09eb9 => github.com/pdesgarets/docker-volume-rbd v0.0.0-20240619160931-588d3b276faa
)
