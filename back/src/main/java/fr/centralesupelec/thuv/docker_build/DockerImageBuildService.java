package fr.centralesupelec.thuv.docker_build;

import com.google.protobuf.ByteString;
import fr.centralesupelec.gRPC.DockerImageRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageBuildRepository;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.service.DockerImageResponseStreamObserver;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DockerImageBuildService {
    private static final Logger logger = LoggerFactory.getLogger(DockerImageBuildService.class);
    private final Map<String, ManagedChannel> channels;

    private final Map<String, StreamObserver<DockerImageRequest>> requestObservers = new HashMap<>();
    private final Map<String, DockerImageResponseStreamObserver> responseObservers = new HashMap<>();

    private final DockerImageBuildRepository dockerImageBuildRepository;
    private final ClusterRepository clusterRepository;
    private Map<String, Boolean> shouldInitializeStubMap = new HashMap<>();

    @Value("${context_save_path}")
    private String contextSavePath;

    @Autowired
    public DockerImageBuildService(
        Map<String, ManagedChannel> channels,
        DockerImageBuildRepository dockerImageBuildRepository,
        ClusterRepository clusterRepository
    ) {
        this.channels = channels;
        this.dockerImageBuildRepository = dockerImageBuildRepository;
        this.clusterRepository = clusterRepository;
    }

    @PostConstruct
    public void init() {
        List<Cluster> clusters = clusterRepository.findAll();

        for (Cluster cluster : clusters) {
            shouldInitializeStubMap.put(cluster.getName(), true);
            createNewStubIfNeeded(cluster);
        }
    }

    public void setShouldInitializeStub(String cluster, Boolean shouldInitializeStub) {
        this.shouldInitializeStubMap.put(cluster, shouldInitializeStub);
    }

    public void createNewStub(Cluster cluster) {
        String clusterName = cluster.getName();

        DockerImageResponseStreamObserver responseObserver =
            new DockerImageResponseStreamObserver(dockerImageBuildRepository, cluster);

        responseObserver.setDockerImageBuildService(this);

        containerServiceGrpc.containerServiceStub asyncStub =
            containerServiceGrpc.newStub(channels.get(clusterName));

        StreamObserver<DockerImageRequest> requestObserver =
            asyncStub.buildDockerImage(responseObserver);

        responseObservers.put(clusterName, responseObserver);
        requestObservers.put(clusterName, requestObserver);
    }

    public void createNewStubIfNeeded(Cluster cluster) {
        ManagedChannel channel = channels.get(cluster.getName());
        logger.debug("shouldInitializeStub : " + shouldInitializeStubMap.get(cluster.getName()));
        if (!shouldInitializeStubMap.get(cluster.getName())) {
            return;
        }
        ConnectivityState state = channel.getState(true);
        logger.debug("Channel state : " + state);

        if (ConnectivityState.READY == state) {
            createNewStub(cluster);
            shouldInitializeStubMap.put(cluster.getName(), false);
            return;
        }

        ConnectivityState updatedState = channel.getState(true);
        logger.debug("Update channel state :" + updatedState);

        if (ConnectivityState.CONNECTING == updatedState) {
            channel.notifyWhenStateChanged(ConnectivityState.CONNECTING, () -> {
                ConnectivityState newState = channel.getState(false);
                logger.debug("Channel state changed from CONNECTING to : " + newState);

                if (ConnectivityState.READY == newState) {
                    createNewStub(cluster);
                    shouldInitializeStubMap.put(cluster.getName(), false);
                }
            });
        }
    }

    public void requestDockerImageBuild(DockerImage dockerImage, DockerImageBuild dockerImageBuild, Cluster cluster) throws IOException {
        DockerImageRequest.Builder builder = DockerImageRequest.newBuilder();
        builder
                .setBuildId(
                        String.valueOf(dockerImageBuild.getId())
                )
                .setDockerfile(
                        sanitizeFileContent(dockerImage.getDockerFile())
                )
                .setWrapperScript(
                        sanitizeFileContent(dockerImage.getWrapperScript())
                )
                .setName(
                        dockerImage.getName()
                );
        if (dockerImage.getContextFolderName() != null) {
            Path path = Paths.get(contextSavePath + dockerImage.getContextFolderName());
            InputStream inputStream = Files.newInputStream(path);
            builder.setContextZip(
                    ByteString.copyFrom(
                            inputStream.readAllBytes()
                    )
            );
        }

        createNewStubIfNeeded(cluster);
        StreamObserver<DockerImageRequest> observer = requestObservers.get(cluster.getName());
        if (observer != null) {
            try {
                observer.onNext(builder.build());
            } catch (Exception e) {
                logger.error("Error configuring container status.\n Please restart the go API then this service.");
                observer.onError(e);
                throw e;
            }
        } else {
            logger.error("No observer found for cluster: " + cluster.getName());
        }
    }

    private String sanitizeFileContent(String fileContent) {
        return fileContent
                .replace("\r\n", "\n")
                .replace("\r", "");
    }
}
