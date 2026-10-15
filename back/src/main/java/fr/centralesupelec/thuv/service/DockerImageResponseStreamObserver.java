package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.DockerImageResponse;
import fr.centralesupelec.thuv.docker_build.DockerImageBuildService;
import fr.centralesupelec.thuv.docker_build.model.BuildStatus;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageBuildRepository;
import fr.centralesupelec.thuv.model.Cluster;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@RequiredArgsConstructor
public class DockerImageResponseStreamObserver implements StreamObserver<DockerImageResponse> {
    public static final String NO_SUITABLE_NODE = "no suitable node";
    private final Logger logger = LoggerFactory.getLogger(ContainerStatusResponseStreamObserver.class);
    // // Not final to avoid circular dependency
    private DockerImageBuildService dockerImageBuildService;

    private final DockerImageBuildRepository dockerImageBuildRepository;
    private final Cluster cluster;

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
        dockerImageBuildService.setShouldInitializeStub(cluster.getName(), true);
        Sentry.captureMessage("Error on build docker image stream.");
    }

    public void onCompleted() {}

    public void setDockerImageBuildService(DockerImageBuildService dockerImageBuildService) {
        this.dockerImageBuildService = dockerImageBuildService;
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
