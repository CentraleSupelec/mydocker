#!/usr/bin/env bash

set -e

echo "Checking service dependencies..."
wait-for-it.sh ${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/thuv} && echo "PostgreSQL is ready"
wait-for-it.sh ${APP_CAS_RSCURL:-http://localhost:9443/cas/p3/serviceValidate} && echo "CAS is ready"
wait-for-it.sh ${SPRING_MAIL_HOST:-127.0.0.1} ${SPRING_MAIL_PORT:-1025} && echo "Mailhog is ready"
wait-for-it.sh ${GO_APP_ADDRESS:-localhost} ${GO_APP_PORT:-50000} && echo "GoApp is ready"

echo "👨‍💻 Development mode: Enabling hot reload..."

find src/main/java -type f | entr -n -r ./mvnw compile &
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
