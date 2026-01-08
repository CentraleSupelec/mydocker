package fr.centralesupelec.thuv.docker_build;

import com.google.protobuf.ByteString;
import fr.centralesupelec.gRPC.DockerImageRequest;
import fr.centralesupelec.gRPC.DockerImageResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.docker_build.model.BuildStatus;
import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageBuildRepository;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
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
import java.util.Optional;

@Service
public class DockerImageBuildService {
    private static final Logger logger = LoggerFactory.getLogger(DockerImageBuildService.class);
    private final ManagedChannel channel;
    private final DockerImageBuildRepository dockerImageBuildRepository;
    private StreamObserver<DockerImageRequest> dockerImageRequestStreamObserver;
    @Value("${context_save_path}")
    private String contextSavePath;

    @Autowired
    public DockerImageBuildService(ManagedChannel channel, DockerImageBuildRepository dockerImageBuildRepository) {
        this.channel = channel;
        this.dockerImageBuildRepository = dockerImageBuildRepository;
    }

    @PostConstruct
    public void init() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);
        this.dockerImageRequestStreamObserver = asyncStub.buildDockerImage(new StreamObserver<>() {

            public void onNext(DockerImageResponse dockerImageResponse) {
                try {
                    logger.debug("Receive response " + dockerImageResponse.getBuildId());
                    Optional<DockerImageBuild> dockerImageBuildOptional = dockerImageBuildRepository
                            .findById(Long.valueOf(dockerImageResponse.getBuildId()));
                    if (!dockerImageBuildOptional.isPresent()) {
                        logger.error("Cannot find build with id " + dockerImageResponse.getBuildId());
                    }
                    DockerImageBuild dockerImageBuild = dockerImageBuildOptional.get();
                    dockerImageBuild
                            .setStatus(
                                convertStatus(dockerImageResponse.getStatus())
                            )
                            .setImageName(
                                    dockerImageResponse.getName()
                            )
                            .setBuildErrors(
                                    dockerImageResponse.getError()
                            );
                    if (!dockerImageResponse.getLogs().isEmpty()) {
                        dockerImageBuild.setLogs(
                                dockerImageResponse.getLogs()
                        );
                    }
                    dockerImageBuildRepository.saveAndFlush(dockerImageBuild);
                } catch (Exception e) {
                    logger.error("Error while treating response " + e);
                }
            }

            public void onError(Throwable throwable) {
                logger.error("Error on build docker image stream.\n Please restart the go API then this service.");
                Sentry.captureMessage("Error on build docker image stream.");
            }

            public void onCompleted() {}
        });
    }

    public void requestDockerImageBuild(DockerImage dockerImage, DockerImageBuild dockerImageBuild) throws IOException {
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

        dockerImageRequestStreamObserver.onNext(
                builder.build()
        );
    }

    private String sanitizeFileContent(String fileContent) {
        return fileContent
                .replace("\r\n", "\n")
                .replace("\r", "");
    }

    private BuildStatus convertStatus(DockerImageResponse.Status status) {
        if (status.equals(DockerImageResponse.Status.BUILDING)) {
            return BuildStatus.BUILDING;
        } else if (status.equals(DockerImageResponse.Status.DONE)) {
            return BuildStatus.OK;
        } else {
            return BuildStatus.ERROR;
        }
    }
}
