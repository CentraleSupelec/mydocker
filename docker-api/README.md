# MyDocker - Docker API

### Technical Stack

This project was developed with:
- GoLang 1.15 (BackEnd)

### Installation

You need to have a local docker swarm cluster:
```
docker swarm init
```

and to setup an attachable overlay network that will host Caddy and swarm services :

```
docker network create -d overlay --attachable <CaddyOverlayNetwork>
```

and an attachable overlay network for the caddy controller instance to configure the server instances :

```
docker network create -d overlay --subnet=10.200.200.0/24 --attachable caddy-controller
```

Default configuration can be found in `docker-api/config.yml`.  
At launch, actual configuration is loaded from `/etc/docker-api/config.yml` + from current folder (whichever is found first).  
Extra directory can be specified with `-config-path=...`.

Recommended way to use a custom config :
```
cd docker-api
cp config.yml ..
go build
./docker-api -config-path=..
```

#### Run with Docker (> 19.03)

Launch the container :
```
docker-compose up
```

#### _Or_ run without Docker

You need GoLang 1.15 and rados C headers.

To run the application:
```
cd docker-api && go build && ./docker-api
```

#### Run the tests

To run the tests :
```
cd docker-api && go test -tags luminous -v
```

To run the tests selectively :
```
cd docker-api && go test -tags luminous -run 'AutoscaleDownTestSuite' -v
```

Some tests will run only in an environment where Docker is installed as a Swarm manager and will be skipped otherwise.

#### Use debugger with Docker

To use the debugger, configure remote debugger in [Intellij](https://www.jetbrains.com/help/go/attach-to-running-go-processes-with-debugger.html#attach-to-a-process-on-a-remote-machine) (or the ones for your IDE). Then comment the default `bash` script (`go build...`) in [docker-compose.yml](./docker-compose.yml) and uncomment the delve line (`go get...`).

Launch the app :
```
docker-compose up
```

Then launch the remote debugger in Intellij

### Caveats for specific features

- To use `volume` features, the volume plugin `centralesupelec/mydockervolume` must be installed and enabled, with one of the two configurations :
  - `DRIVER_MODE=FS` : volumes will be stored on the server in `/mnt/mydocker-fs`
  - `DRIVER_MODE=RBD` : volumes will be stored on a Ceph cluster that must be configured with other plugin parameters and files in `/etc/ceph`

The `saveData` feature will work only if the driver is installed in mode `RBD`.

In both cases, directories `/mnt/mydocker-fs` and `/etc/ceph` must exist prior to enabling the plugin.

### Developing with volumes on macOS

For Docker Desktop on macOS : in modern versions of macOS, it is not allowed to create `/mnt/mydocker-fs`. You must use another Docker engine, e.g. [colima](https://github.com/abiosoft/colima), with following customization :
1. in `$HOME/.colima/default/colima.yaml`, replace `provision: []` with
```yaml
provision:
  - mode: system
    script: mkdir -p /etc/ceph
  - mode: system
    script: mkdir -p /mnt/mydocker-fs
```
2. launch colima with an IP address : `colima start --network-address`
3. Retrieve the IP address : `colima status`
4. start the local swarm with the address : `docker swarm init --advertise-addr=...`
5. Install the plugin: `docker plugin install --disable centralesupelec/mydockervolume`
6. Configure the plugin : `docker plugin set centralesupelec/mydockervolume DRIVER_MODE=FS`
7. Then, launch the Go app : `docker-compose up`

### Manually stop scaleUp / scaleDown

Configure StopScaleUpFilePath / StopScaleDownFilePath. Each time autoscaleUp(Down) is run, it will first check if the corresponding stop file exists. If found, the autoscaling will not proceed.

### Accessing containers through caddy-reverse-proxy

After creating a container, you can access it through <random_username>.<ReverseProxyUrl>

For example :

```
curl -k --resolve silly_sammet.ovhcaddy.com:443:127.0.0.1 https://silly_sammet.ovhcaddy.com/
```

To be able to access the reverse proxy in your browser, you need to edit your hosts file and add :

```
127.0.0.1 *.<ReverseProxyUrl>
```

**NOTE :**

Some operating systems don't support wildcard entries like *.<ReverseProxyUrl>. They only supports exact domain mappings. So if you need to access your container in your browser, you have to add it specifically :

```
127.0.0.1 <random_username>.<ReverseProxyUrl>
```
