package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerRequest;
import fr.centralesupelec.gRPC.ContainerResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.locks.ReentrantLock;

@Service
public class RequestContainerService {
    private final ManagedChannel channel;
    private StreamObserver<ContainerRequest> containerRequestStreamObserver;
    private final StreamObserver<ContainerResponse> containerResponseStreamObserver;
    private final ReentrantLock lock = new ReentrantLock();
    private final ActivityLogger activityLogger;

    @Autowired
    public RequestContainerService(
            ManagedChannel channel,
            StreamObserver<ContainerResponse> containerResponseStreamObserver,
            ActivityLogger activityLogger
    ) {
        this.channel = channel;
        this.containerResponseStreamObserver = containerResponseStreamObserver;
        this.activityLogger = activityLogger;
    }

    @PostConstruct
    public void init() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);
        this.containerRequestStreamObserver = asyncStub.getContainer(this.containerResponseStreamObserver);
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
