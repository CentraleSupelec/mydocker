package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerStatusRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
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

    @PostConstruct
    public void init() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);
        containerStatusResponseStreamObserver.setContainerStatusConfigureService(this);
        this.containerStatusRequestStreamObserver = asyncStub.getContainerStatus(containerStatusResponseStreamObserver);
    }

    public void configureContainerStatus(String courseID, String userID, ContainerStatusRequest.Action action) {
        ContainerStatusRequest request = ContainerStatusRequest.newBuilder()
                .setCourseID(courseID)
                .setUserID(userID)
                .setAction(action)
                .build();
        try {
            lock.lock();
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
