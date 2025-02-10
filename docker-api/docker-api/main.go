package main

import (
	"flag"
	"io"
	"math/rand"
	"net"
	_ "net/http/pprof"
	"strconv"
	"time"

	"github.com/go-co-op/gocron"
	"github.com/spf13/viper"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"

	"net/http"

	"github.com/docker/cli/cli/connhelper"

	"github.com/docker/docker/client"
	"google.golang.org/grpc"

	"github.com/Workiva/go-datastructures/queue"
	log "github.com/sirupsen/logrus"
	"fmt"
	"strings"
)

type config struct {
	DockerConfig            dockerConfig
	AdminImage              string
	Port                    int
	Worker                  int
	PortThreshold           int64
	PortSize                int64
	PortMin                 uint32
	PortMax                 uint32
	DefaultNanoCPUsLimit    int64
	DefaultMemoryBytesLimit int64
	BuildRegistryAddress    string
	BuildImageRepository    string
	BuildImage              string
	LogLevel                string
	CephFSPrefix            string
	CephServiceName         string
	MaxRecvMsgSize          int
	CephCluster             string
	KeyringUser             string
	CephPool                string
	DeployImage             string
	Environment             string
	RegistryCredentials     []registryCredential
	AutoscalingStateBaseUrl string
	ContainerStatusInterval string
	ScaleUpInterval         string
	ScaleUpCooldown         string
	ScaleDownInterval       string
	EmailUsername           string
	EmailPassword           string
	EmailServer             string
	EmailPort               int
	EmailFrom               string
	EmailTo                 []string
	LogsTimeout             string
	ScaleDownRemoveNonEmpty bool
	DeployEnvSecrets        []string
	PrecreateVolume         bool
	StopScaleUpFilePath     string
	StopScaleDownFilePath   string
	ReverseProxyUrl         string
	CaddyOverlayNetwork     string
	CaddyTlsInternal        bool
	CaddyTlsCertificatePath string
	CaddyTlsKeyPath         string
	CaddyStreamCloseDelay   string
	StudentVolumeSize       int
	LogsTimestamps          bool
	LogsDetails             bool
}

type dockerConfig struct {
	Host string
}

type registryCredential struct {
	Username string
	Password string
	Address  string
}

const defaultMaxRecvMsgSize = 5 * 1024 * 1024 * 1024

var c config

type server struct {
	dockerClient *client.Client
	ports        *queue.Queue
	pb.UnimplementedContainerServiceServer
	cronScheduler *gocron.Scheduler
}

func init() {
	rand.Seed(time.Now().UTC().UnixNano())
}

func (s *server) GetContainer(stream pb.ContainerService_GetContainerServer) error {
	// chan for input request and response
	in, out := make(chan *pb.ContainerRequest, c.Worker), make(chan *pb.ContainerResponse, c.Worker)
	done := make(chan struct{})
	createRbdImageIn, createRbdImageOut := make(chan CreateRbdImageWorkerRequest, c.Worker), make(chan CreateRbdImageWorkerResponse, c.Worker)
	var workersRbdImageOut []chan CreateRbdImageWorkerResponse

	// Create Go worker
	for w := 0; w < c.Worker; w++ {
		workerRbdImageOut := make(chan CreateRbdImageWorkerResponse, c.Worker)
		workersRbdImageOut = append(workersRbdImageOut, workerRbdImageOut)
		go getOrCreateContainer(in, out, s.dockerClient, s.ports, createRbdImageIn, workerRbdImageOut, w)
		go CreateRbdImageWorker(createRbdImageIn, createRbdImageOut)
	}

	go func() {
		for {
			select {
			case data := <-out:
				_ = stream.Send(data)
			case <-done:
				return
			}
		}
	}()

	// Send createRbdImageOut to worker
	go func() {
		for r := range createRbdImageOut {
			if len(workersRbdImageOut) > r.workerIndex {
				workersRbdImageOut[r.workerIndex] <- r
			} else {
				log.Errorf("failed to send create rdb image response to worker for service name %s, with workerId %d", r.imageName, r.workerIndex)
			}
		}
	}()

	for {
		req, err := stream.Recv()
		if err == io.EOF {
			close(in)
			close(done)
			break
		} else if err != nil {
			log.Error(err)
			return err
		}
		in <- req
	}
	return nil
}
func (s *server) SaveData(stream pb.ContainerService_SaveDataServer) error {
	// chan for input request and response
	in, out := make(chan *pb.SaveDataRequest, c.Worker), make(chan *pb.SaveDataResponse, c.Worker)
	done := make(chan struct{})

	// Create Go worker
	for w := 1; w <= c.Worker; w++ {
		go saveData(in, out, s.dockerClient)
	}

	go func() {
		for {
			select {
			case data := <-out:
				_ = stream.Send(data)
			case <-done:
				return
			}
		}
	}()

	for {
		req, err := stream.Recv()
		if err == io.EOF {
			close(in)
			close(done)
			break
		} else if err != nil {
			log.Error(err)
			return err
		}
		in <- req
	}
	return nil
}

func (s *server) GetAdminContainer(stream pb.ContainerService_GetAdminContainerServer) error {
	// chan for input request and response
	in, out := make(chan *pb.AdminContainerRequest, c.Worker), make(chan *pb.AdminContainerResponse, c.Worker)
	done := make(chan struct{})

	// Create Go worker
	go getOrCreateAdminContainer(in, out, s.dockerClient, s.ports)

	go func() {
		for {
			select {
			case data := <-out:
				_ = stream.Send(data)
			case <-done:
				return
			}
		}
	}()

	for {
		req, err := stream.Recv()
		if err == io.EOF {
			close(in)
			close(done)
			break
		} else if err != nil {
			log.Error(err)
			return err
		}
		in <- req
	}
	return nil
}

func (s *server) GetContainerStatus(stream pb.ContainerService_GetContainerStatusServer) error {
	// chan for input request and response
	in, out := make(chan *pb.ContainerStatusRequest), make(chan *pb.ContainerStatusResponse)
	done := make(chan struct{})

	containersToWatch := map[string]bool{}

	// Create Go worker
	go configureContainerStatus(in, containersToWatch)
	go setupContainerStatusCron(out, containersToWatch, s.dockerClient, s.cronScheduler)

	go func() {
		for {
			select {
			case data := <-out:
				_ = stream.Send(data)
			case <-done:
				return
			}
		}
	}()

	for {
		req, err := stream.Recv()
		if err == io.EOF {
			close(in)
			close(done)
			break
		} else if err != nil {
			log.Error(err)
			return err
		}
		in <- req
	}
	return nil
}

func (s *server) BuildDockerImage(stream pb.ContainerService_BuildDockerImageServer) error {
	// chan for input request and response
	in, out := make(chan *pb.DockerImageRequest, c.Worker), make(chan *pb.DockerImageResponse, c.Worker)
	done := make(chan struct{})

	for w := 1; w <= 5; w++ {
		go buildDockerImageWorker(in, out, s.dockerClient)
	}

	go func() {
		for {
			select {
			case data := <-out:
				err := stream.Send(data)
				if err != nil {
					log.Errorf("Got an while responding build docker image %s", err)
				}
			case <-done:
				return
			}
		}
	}()

	for {
		req, err := stream.Recv()
		if err == io.EOF {
			log.Error(err)
			close(in)
			close(done)
			break
		} else if err != nil {
			log.Error(err)
			return err
		}
		in <- req
	}
	return nil
}

func (s *server) ShutdownContainer(stream pb.ContainerService_ShutdownContainerServer) error {
	// chan for input request and response
	in, out := make(chan *pb.ShutdownContainerRequest, c.Worker), make(chan *pb.ShutdownContainerResponse, c.Worker)
	done := make(chan struct{})

	for w := 1; w <= 5; w++ {
		go shutdownContainer(in, out, s.dockerClient)
	}

	go func() {
		for {
			select {
			case data := <-out:
				err := stream.Send(data)
				if err != nil {
					log.Errorf("Got an error while responding shutdown %s", err)
				}
			case <-done:
				return
			}
		}
	}()

	for {
		req, err := stream.Recv()
		if err == io.EOF {
			log.Error(err)
			close(in)
			close(done)
			break
		} else if err != nil {
			log.Error(err)
			return err
		}
		in <- req
	}
	return nil
}

func NewDockerClient(dockerConfig dockerConfig) (*client.Client, error) {
	dockerHost := dockerConfig.Host
	if dockerHost != "" {
		if (!strings.HasPrefix(dockerHost, "ssh://")) {
			return nil, fmt.Errorf("only ssh hosts are allowed, got: %s", dockerHost)
		}
		helper, err := connhelper.GetConnectionHelper(dockerHost)
		if err != nil {
			return nil, err
		}

		httpClient := &http.Client{
			Transport: &http.Transport{
				DialContext: helper.Dialer,
			},
		}

		var clientOpts []client.Opt

		clientOpts = append(clientOpts,
			client.WithHTTPClient(httpClient),
			client.WithHost(helper.Host),
			client.WithDialContext(helper.Dialer),
			client.WithVersion("1.44"),
		)
		cli, err := client.NewClientWithOpts(clientOpts...)
		if err != nil {
			return nil, err
		}
		return cli, nil
	}
	cli, err := client.NewClientWithOpts(client.WithVersion("1.44"))
	if err != nil {
		return nil, err
	}
	return cli, nil
}

func main() {
	configPath := flag.String(
		"config-path",
		"",
		"extra location of config.yml",
	)
	flag.Parse()
	viper.SetConfigName("config")           // name of config file (without extension)
	viper.SetConfigType("yaml")             // REQUIRED if the config file does not have the extension in the name
	viper.AddConfigPath("/etc/docker-api/") // path to look for the config file in
	if *configPath != "" {
		viper.AddConfigPath(*configPath)
	}
	viper.AddConfigPath(".") // optionally look for config in the working directory
	viper.SetDefault("MaxRecvMsgSize", defaultMaxRecvMsgSize)
	viper.SetDefault("BuildImageRepository", "dev")
	viper.SetDefault("StopScaleUpFilePath", "/etc/docker-api/stop-scale-up")
	viper.SetDefault("StopScaleDownFilePath", "/etc/docker-api/stop-scale-down")
	viper.SetDefault("ReverseProxyUrl", "localhost")
	viper.SetDefault("CaddyOverlayNetwork", "caddy-overlay")
	viper.SetDefault("CaddyTlsInternal", false)
	viper.SetDefault("CaddyTlsCertificatePath", "/etc/ssl/caddy_reverse_proxy/cert.pem")
	viper.SetDefault("CaddyTlsKeyPath", "/etc/ssl/caddy_reverse_proxy/key.pem")
	viper.SetDefault("CaddyStreamCloseDelay", "4h")
	viper.SetDefault("ContainerStatusInterval", "1s")
	viper.SetDefault("LogsTimestamps", true)
	viper.SetDefault("LogsDetails", false)
	if err := viper.ReadInConfig(); err != nil {
		if _, ok := err.(viper.ConfigFileNotFoundError); ok {
			log.Panicf("Failed to find config file: %v", err)
		} else {
			log.Panic(err)
		}
	}
	err := viper.Unmarshal(&c)
	if err != nil {
		log.Panic(err)
	}

	level, err := log.ParseLevel(c.LogLevel)
	if err != nil {
		level = log.WarnLevel
	}
	log.SetLevel(level)

	//docker client
	cli, err := NewDockerClient(c.DockerConfig)
	if err != nil {
		log.Panicf("Erreur de connexion à Docker: %s", err)
	}

	// chan for port and launch port worker
	ports := queue.New(c.PortSize)
	go portsWorker(ports, cli)

	// create cron job
	cronS := gocron.NewScheduler(time.UTC)
	cronS.SingletonModeAll()
	addCleaningCron(cronS, cli)
	cronS.StartAsync()

	// Create gRPC server
	binding := ":" + strconv.Itoa(c.Port)
	lis, err := net.Listen("tcp", binding)
	if err != nil {
		log.Panicf("failed to listen: %v", err)
	}
	log.Debugf("Listening on %s", binding)
	s := grpc.NewServer(
		grpc.MaxRecvMsgSize(c.MaxRecvMsgSize),
	)
	pb.RegisterContainerServiceServer(s, &server{dockerClient: cli, ports: ports, cronScheduler: cronS})
	if err := s.Serve(lis); err != nil {
		log.Panicf("failed to serve: %v", err)
	}
}
