FROM eclipse-temurin:17-jdk
RUN apt-get update && apt-get install -y \
    openssl \
    netcat-openbsd \
    curl \
    entr \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY lombok.config .

RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY . .

RUN openssl genrsa -traditional -out src/main/resources/private_key.pem 4096 \
    && openssl rsa -traditional -pubout -in src/main/resources/private_key.pem -out src/main/resources/public_key.pem \
    && chmod 644 src/main/resources/private_key.pem && chmod 644 src/main/resources/public_key.pem

COPY docker/files/wait-for-it.sh /usr/local/bin/wait-for-it.sh
COPY docker/files/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/wait-for-it.sh /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl http://localhost:8080 || exit 1

ENTRYPOINT [ "/usr/local/bin/docker-entrypoint.sh" ]
