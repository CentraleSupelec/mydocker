## Protobuf binding regeneration.
##
## The committed bindings under docker-api/protobuf are generated with an exact
## toolchain; regenerating with a different one rewrites the whole file and buries
## the real change in churn. The versions below are the ones recorded in the
## headers of the committed files, so they are the contract:
##
##   protoc              26.1   (reported as v5.26.1)
##   protoc-gen-go       v1.33.0
##   protoc-gen-go-grpc  v1.3.0
##
## Everything runs in a container because this repository does not build on macOS
## (it needs the ceph development libraries).

PROTOC_VERSION      ?= 26.1
PROTOC_GEN_GO        ?= v1.33.0
PROTOC_GEN_GO_GRPC   ?= v1.3.0
GO_IMAGE             ?= golang:1.25
PROTO_OUT            ?= docker-api/protobuf

.PHONY: proto proto-check

## Regenerate the Go binding from proto/containerService.proto.
proto:
	docker run --rm -v "$(CURDIR)":/src -w /src $(GO_IMAGE) bash -eu -c '\
		arch=$$(dpkg --print-architecture); \
		case "$$arch" in \
			arm64) protoc_arch=aarch_64 ;; \
			amd64) protoc_arch=x86_64 ;; \
			*) echo "unsupported architecture $$arch" >&2; exit 1 ;; \
		esac; \
		apt-get update -qq && apt-get install -y -qq unzip curl >/dev/null; \
		curl -sSLo /tmp/protoc.zip \
			"https://github.com/protocolbuffers/protobuf/releases/download/v$(PROTOC_VERSION)/protoc-$(PROTOC_VERSION)-linux-$$protoc_arch.zip"; \
		unzip -q -o /tmp/protoc.zip -d /usr/local; \
		go install google.golang.org/protobuf/cmd/protoc-gen-go@$(PROTOC_GEN_GO); \
		go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@$(PROTOC_GEN_GO_GRPC); \
		PATH="$$PATH:$$(go env GOPATH)/bin" protoc \
			-I proto \
			--go_out=paths=source_relative:$(PROTO_OUT) \
			--go-grpc_out=paths=source_relative:$(PROTO_OUT) \
			proto/containerService.proto; \
		chown -R $$(stat -c "%u:%g" /src) $(PROTO_OUT)'

## Fail if the committed binding does not match the proto.
proto-check: proto
	git diff --exit-code -- $(PROTO_OUT)
