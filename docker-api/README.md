# MyDocker - Docker API

### Technical Stack

This project was developed with:
- GoLang 1.15 (BackEnd)

### Installation

You need to have a local docker swarm cluster:
```
docker swarm init
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
