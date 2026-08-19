# golang:1.22 has an 9.1 CVE
# No --platform=$BUILDPLATFORM here: the driver is cgo (go-ceph/librados), so
# the binary cannot be cross-compiled with GOARCH alone. The builder must run
# on the target platform (qemu-emulated when building amd64 from an M Mac).
FROM golang:1.23 AS builder

# Ceph libs come from debian bookworm itself: pacific 16.2.x, the version both
# prod and preprod actually run. The old download.ceph.com reef repo line was
# inert on bookworm (no bookworm suite published) and apt silently resolved
# these same bookworm packages while the image claimed reef. A real Ceph
# version bump is its own change with its own test cycle.
RUN apt-get update && apt-get install -yq \
        libcephfs-dev librbd-dev librados-dev \
    && rm -rf /var/lib/apt/lists/*

COPY . /usr/src/mydockervolume
WORKDIR /usr/src/mydockervolume
RUN go install -v
CMD ["/go/bin/mydockervolume"]

#FROM --platform=linux/amd64 debian:bookworm
#FROM --platform=linux/arm64 debian:bookworm # for arm64 when building for M Mac
FROM debian:bookworm

RUN apt-get update && apt-get install -yq \
        libcephfs-dev librbd-dev librados-dev \
        ceph-common \
        xfsprogs \
        kmod vim acl \
    && mkdir -p /run/docker/plugins /mnt/state /mnt/volumes /etc/ceph \
    && rm -rf /var/lib/apt/lists/*

# rbd shim: caps a hung `rbd unmap` (see scripts/rbd-shim.sh)
RUN mv /usr/bin/rbd /usr/bin/rbd.real
COPY scripts/rbd-shim.sh /usr/bin/rbd
RUN chmod 755 /usr/bin/rbd

RUN mkdir -p /run/docker/plugins /mnt/mydocker-fs
COPY --from=builder /go/bin/mydockervolume /mydockervolume
CMD ["/mydockervolume"]
