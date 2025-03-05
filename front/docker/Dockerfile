FROM node:18
RUN apt-get update && apt-get install -y gettext

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install --legacy-peer-deps

COPY . .

COPY docker/files/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 4200

HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl http://localhost:4200 || exit 1

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["npm", "run", "start"]
