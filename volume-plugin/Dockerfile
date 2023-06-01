FROM golang:1.16 as builder

ENV CEPH_VERSION pacific
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

FROM debian:bookworm
ENV CEPH_VERSION quincy

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
        kmod vim \
    && mkdir -p /run/docker/plugins /mnt/state /mnt/volumes /etc/ceph \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /run/docker/plugins /mnt/mydocker-fs
COPY --from=builder /go/bin/mydockervolume /mydockervolume
CMD ["/mydockervolume"]
