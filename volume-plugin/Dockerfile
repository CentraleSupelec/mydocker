#FROM --platform=linux/amd64 golang:1.22 as builder # for arm64 when building for M Mac
#FROM --platform=linux/arm64 golang:1.22 as builder
# golang:1.22 has an 9.1 CVE
FROM --platform=$BUILDPLATFORM golang:1.23 AS builder


ENV CEPH_VERSION=reef
RUN apt-get update && apt-get install -yq software-properties-common wget gnupg2 \
    && wget -q -O- 'https://download.ceph.com/keys/release.asc' | apt-key add - \
    && apt-add-repository "deb https://download.ceph.com/debian-$CEPH_VERSION/ $(lsb_release -sc) main" \
    && rm -rf /var/lib/apt/lists/*

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

ENV CEPH_VERSION=reef

RUN apt-get update && apt-get install -yq software-properties-common wget gnupg2 \
    && wget -q -O- 'https://download.ceph.com/keys/release.asc' | apt-key add - \
    && apt-add-repository "deb https://download.ceph.com/debian-$CEPH_VERSION/ $(lsb_release -sc) main" \
    && rm -rf /var/lib/apt/lists/*

RUN apt-get update && apt-get install -yq \
        libcephfs-dev librbd-dev librados-dev \
    && rm -rf /var/lib/apt/lists/*

RUN apt-get update && apt-get install -yq \
        ceph-common \
        xfsprogs \
        kmod vim acl \
    && mkdir -p /run/docker/plugins /mnt/state /mnt/volumes /etc/ceph \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /run/docker/plugins /mnt/mydocker-fs
COPY --from=builder /go/bin/mydockervolume /mydockervolume
CMD ["/mydockervolume"]
