package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ShutdownContainerResponse;
import fr.centralesupelec.thuv.dtos.ShutdownContainerDto;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.storage.ShutdownStatusStorage;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


@RequiredArgsConstructor
public class ShutdownContainerResponseStreamObserver implements StreamObserver<ShutdownContainerResponse> {
    public static final String NO_SUITABLE_NODE = "no suitable node";
    private final Logger logger = LoggerFactory.getLogger(ContainerStatusResponseStreamObserver.class);
    // Not final to avoid circular dependency
    private ShutdownContainerService shutdownContainerService;
    private final ShutdownStatusStorage shutdownStatusStorage;
    private final Cluster cluster;

    public void onNext(ShutdownContainerResponse shutdownContainerResponse) {
        logger.debug("Received response");
        if (shutdownContainerResponse.getError().equals("")) {
            logger.debug("No error");
            Optional<ShutdownContainerDto> shutdownContainer = shutdownStatusStorage.getContainer(
                    shutdownContainerResponse.getUserID(),
                    shutdownContainerResponse.getCourseID()
            );
            shutdownContainer.ifPresent(container -> container.setIsShutdown(true));
        } else {
            logger.debug(
                    "Error while shutdowning for course {} and student {} : {}",
                    shutdownContainerResponse.getCourseID(),
                    shutdownContainerResponse.getUserID(),
                    shutdownContainerResponse.getError()
            );
            Optional<ShutdownContainerDto> shutdownContainer = shutdownStatusStorage.getContainer(
                    shutdownContainerResponse.getUserID(),
                    shutdownContainerResponse.getCourseID()
            );
            shutdownContainer.ifPresent(container -> container.setError(shutdownContainerResponse.getError()));
        }
    }

    public void onError(Throwable throwable) {
        logger.error("Stream error");
        if (logger.isDebugEnabled()) {
            throwable.printStackTrace();
        }
        Sentry.captureMessage("Error in shutdown stream");
        shutdownContainerService.setShouldInitializeStub(cluster.getName(), true);
    }

    public void onCompleted() {
        logger.debug("Completed");
    }

    public void setShutdownContainerService(ShutdownContainerService shutdownContainerService) {
        this.shutdownContainerService = shutdownContainerService;
    }
}
