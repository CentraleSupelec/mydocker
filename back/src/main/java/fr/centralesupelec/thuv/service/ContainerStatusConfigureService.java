package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerStatusRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
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
public class ContainerStatusConfigureService {
    private static final Logger logger = LoggerFactory.getLogger(ContainerStatusConfigureService.class);
    private StreamObserver<ContainerStatusRequest> containerStatusRequestStreamObserver;
    private final ManagedChannel channel;
    private final ReentrantLock lock = new ReentrantLock();
    private final ContainerStatusResponseStreamObserver containerStatusResponseStreamObserver;
    private boolean shouldInitializeStub = true;

    public void setShouldInitializeStub(boolean shouldInitializeStub) {
        this.shouldInitializeStub = shouldInitializeStub;
    }

    @PostConstruct
    public void init() {
        createNewStubIfNeeded();
    }

    public void createNewStub() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);
        containerStatusResponseStreamObserver.setContainerStatusConfigureService(this);
        this.containerStatusRequestStreamObserver = asyncStub.getContainerStatus(containerStatusResponseStreamObserver);
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

    public void configureContainerStatus(String courseID, String userID, ContainerStatusRequest.Action action) {
        ContainerStatusRequest request = ContainerStatusRequest.newBuilder()
                .setCourseID(courseID)
                .setUserID(userID)
                .setAction(action)
                .build();
        try {
            lock.lock();
            createNewStubIfNeeded();
            containerStatusRequestStreamObserver.onNext(request);
        } catch (RuntimeException e) {
            logger.error("Error configuring container status.\n Please restart the go API then this service.");
            containerStatusRequestStreamObserver.onError(e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

}
