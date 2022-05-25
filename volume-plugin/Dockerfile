FROM golang:1.15-alpine as builder
COPY . /usr/src/mydockervolume
WORKDIR /usr/src/mydockervolume
RUN go install -v
CMD ["/go/bin/mydockervolume"]

FROM alpine
RUN mkdir -p /run/docker/plugins /mnt/mydocker-fs
COPY --from=builder /go/bin/mydockervolume .
CMD ["mydockervolume"]
