#!/bin/bash
protoc --go-grpc_out=../docker-api/protobuf/ --go_out=../docker-api/protobuf/ containerService.proto
