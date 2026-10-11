package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;
@RequiredArgsConstructor
@Service
public class RequestContainerService {
    private static final Logger logger = LoggerFactory.getLogger(RequestContainerService.class);
    private final ManagedChannel channel;
    private StreamObserver<ContainerRequest> containerRequestStreamObserver;
    private final ContainerResponseStreamObserver containerResponseStreamObserver;

    private final ReentrantLock lock = new ReentrantLock();
    private final ActivityLogger activityLogger;
    private boolean shouldInitializeStub = true;

    @PostConstruct
    public void init() {
        createNewStubIfNeeded();
    }

    public void setShouldInitializeStub(boolean shouldInitializeStub) {
        this.shouldInitializeStub = shouldInitializeStub;
    }
    
    public void createNewStub() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);
        containerResponseStreamObserver.setRequestContainerService(this);
        this.containerRequestStreamObserver = asyncStub.getContainer(containerResponseStreamObserver);
    }

    public void createNewStubIfNeeded() {
        logger.debug("shouldInitializeStub : " + shouldInitializeStub);
        if (!shouldInitializeStub) {
            return;
        }
        ConnectivityState state = channel.getState(true);
        logger.debug("Channel state : " + state);

        if (ConnectivityState.READY == state) {
            createNewStub();
            shouldInitializeStub = false;
            return;
        }

        ConnectivityState updatedState = channel.getState(true);
        logger.debug("Update channel state :" + updatedState);

        if (ConnectivityState.CONNECTING == updatedState) {
            channel.notifyWhenStateChanged(ConnectivityState.CONNECTING, () -> {
                ConnectivityState newState = channel.getState(false);
                logger.debug("Channel state changed from CONNECTING to : " + newState);
        
                if (ConnectivityState.READY == newState) {
                    createNewStub();
                    shouldInitializeStub = false;
                }
            });
        }
    }

    public void requestContainer(ContainerRequest request) {
        try {
            activityLogger.log(
                    request.getOptions().getForceRecreate()
                            ? LogAction.ENVIRONMENT_RESTART
                            : LogAction.ENVIRONMENT_ASK,
                    LogModelName.COURSE,
                    request.getCourseID()
            );
            lock.lock();
            createNewStubIfNeeded();
            containerRequestStreamObserver.onNext(request);
        } catch (RuntimeException e) {
            // Cancel RPC
            containerRequestStreamObserver.onError(e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
