package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.dtos.ShutdownContainerDto;
import fr.centralesupelec.thuv.storage.ShutdownStatusStorage;
import fr.centralesupelec.gRPC.ShutdownContainerRequest;
import fr.centralesupelec.gRPC.ShutdownContainerResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ShutdownContainerService {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownContainerService.class);

    private final ManagedChannel channel;
    private final ShutdownStatusStorage shutdownStatusStorage;
    private StreamObserver<ShutdownContainerRequest> shutdownContainerRequestStreamObserver;
    private final ReentrantLock lock = new ReentrantLock();
    private final ActivityLogger activityLogger;

    @Autowired
    public ShutdownContainerService(
            ManagedChannel channel,
            ShutdownStatusStorage shutdownStatusStorage,
            ActivityLogger activityLogger
    ) {
        this.channel = channel;
        this.shutdownStatusStorage = shutdownStatusStorage;
        this.activityLogger = activityLogger;
    }

    @PostConstruct
    public void init() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);

        this.shutdownContainerRequestStreamObserver = asyncStub.shutdownContainer(new StreamObserver<>() {
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
                    logger.debug(String.format(
                            "Error while shutdowning for course %s and student %s : %s",
                            shutdownContainerResponse.getCourseID(),
                            shutdownContainerResponse.getUserID(),
                            shutdownContainerResponse.getError()
                            ));
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
            }

            public void onCompleted() {
                logger.debug("Completed");

            }
        });
    }

    public void shutdownContainer(String userId, String courseId) {
        activityLogger.log(LogAction.ENVIRONMENT_SHUTDOWN, LogModelName.COURSE, courseId);
        shutdownStatusStorage.addContainer(userId, courseId);
        ShutdownContainerRequest request = ShutdownContainerRequest.newBuilder()
                .setCourseID(courseId)
                .setUserID(userId)
                .build();
        try {
            lock.lock();
            shutdownContainerRequestStreamObserver.onNext(request);
        } catch (RuntimeException e) {
            // Cancel RPC
            shutdownContainerRequestStreamObserver.onError(e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
