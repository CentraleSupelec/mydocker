FROM golang:1.15

RUN apt-get update && apt-get install -y librados-dev librbd-dev

WORKDIR /app

RUN curl -sSfL https://raw.githubusercontent.com/air-verse/air/master/install.sh | sh -s -- -b $(go env GOPATH)/bin

EXPOSE 50000

HEALTHCHECK --interval=5s --timeout=3s --retries=3 \
  CMD nc -z 127.0.0.1 50000 || exit 1

ENTRYPOINT [ "air", "-c", "/app/docker/files/dev/.air.toml" ]
