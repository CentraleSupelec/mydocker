PLUGIN_NAME = centralesupelec/mydockervolume
PLUGIN_VERSION ?= dev

all: clean rootfs create

.PHONY: clean
clean:
	@echo "### rm ./plugin"
	@rm -rf ./plugin

.PHONY: rootfs
rootfs:
	@echo "### docker build: rootfs image with docker-volume-rbd"
	@docker build --platform linux/amd64 -q -t ${PLUGIN_NAME}:rootfs .
	@echo "### create rootfs directory in ./plugin/rootfs"
	@mkdir -p ./plugin/rootfs
	@docker create --name tmp ${PLUGIN_NAME}:rootfs
	@docker export tmp | tar -x --exclude=dev/ -C ./plugin/rootfs
	@echo "### copy config.json to ./plugin/"
	@cp config.json ./plugin/
	@docker rm -vf tmp

.PHONY: create
create:
	@echo "### remove existing plugin ${PLUGIN_NAME}:${PLUGIN_VERSION} if exists"
	@docker plugin rm -f ${PLUGIN_NAME}:${PLUGIN_VERSION} || true
	@echo "### create new plugin ${PLUGIN_NAME}:${PLUGIN_VERSION} from ./plugin"
	@docker plugin create ${PLUGIN_NAME}:${PLUGIN_VERSION} ./plugin

.PHONY: push
push:
	@echo "### push plugin ${PLUGIN_NAME}:${PLUGIN_VERSION}"
	@docker plugin push ${PLUGIN_NAME}:${PLUGIN_VERSION}

.PHONY: enable
enable:
	@echo "### enable plugin ${PLUGIN_NAME}:${PLUGIN_VERSION}"
	@docker plugin enable ${PLUGIN_NAME}:${PLUGIN_VERSION}

.PHONY: upgrade
upgrade:
	@echo "### disable plugin ${PLUGIN_NAME}"
	@docker plugin disable -f ${PLUGIN_NAME}
	@echo "### upgrade plugin ${PLUGIN_NAME} to ${PLUGIN_NAME}:${PLUGIN_VERSION}"
	@docker plugin upgrade ${PLUGIN_NAME} ${PLUGIN_NAME}:${PLUGIN_VERSION}
	@echo "### enable plugin ${PLUGIN_NAME}"
	@docker plugin enable ${PLUGIN_NAME}
