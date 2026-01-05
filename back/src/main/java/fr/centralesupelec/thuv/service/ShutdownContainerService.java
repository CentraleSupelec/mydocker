package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.storage.ShutdownStatusStorage;
import fr.centralesupelec.gRPC.ShutdownContainerRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ShutdownContainerService {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownContainerService.class);

    private final Map<String, ManagedChannel> channels;
    private final ShutdownStatusStorage shutdownStatusStorage;
    private final Map<String, StreamObserver<ShutdownContainerRequest>> requestObservers = new HashMap<>();
    private final Map<String, ShutdownContainerResponseStreamObserver> responseObservers = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ActivityLogger activityLogger;
    private Map<String, Boolean> shouldInitializeStubMap = new HashMap<>();
    private final CourseRepository courseRepository;
    private final ClusterRepository clusterRepository;

    @Autowired
    public ShutdownContainerService(
        Map<String, ManagedChannel> channels,
            ShutdownStatusStorage shutdownStatusStorage,
            ActivityLogger activityLogger,
            CourseRepository courseRepository,
            ClusterRepository clusterRepository
    ) {
        this.channels = channels;
        this.shutdownStatusStorage = shutdownStatusStorage;
        this.activityLogger = activityLogger;
        this.courseRepository = courseRepository;
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

        ShutdownContainerResponseStreamObserver responseObserver =
            new ShutdownContainerResponseStreamObserver(shutdownStatusStorage, cluster);

        responseObserver.setShutdownContainerService(this);

        containerServiceGrpc.containerServiceStub asyncStub =
            containerServiceGrpc.newStub(channels.get(clusterName));

        StreamObserver<ShutdownContainerRequest> requestObserver =
            asyncStub.shutdownContainer(responseObserver);

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

    public void shutdownContainer(String userId, String courseId) {
        activityLogger.log(LogAction.ENVIRONMENT_SHUTDOWN, LogModelName.COURSE, courseId);
        shutdownStatusStorage.addContainer(userId, courseId);
        ShutdownContainerRequest request = ShutdownContainerRequest.newBuilder()
                .setCourseID(courseId)
                .setUserID(userId)
                .build();

        try {
            lock.lock();
            Optional<Course> course = courseRepository.findById(Long.parseLong(courseId));
            if (course.isPresent()) {
                Cluster cluster = course.get().getComputeType().getCluster();
                createNewStubIfNeeded(cluster);
                StreamObserver<ShutdownContainerRequest> observer = requestObservers.get(cluster.getName());
                if (observer != null) {
                    try {
                        observer.onNext(request);
                    } catch (Exception e) {
                        logger.error("Error configuring container status.\n Please restart the go API then this service.");
                        observer.onError(e);
                        throw e;
                    }
                } else {
                    logger.error("No observer found for cluster: " + cluster.getName());
                }
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            logger.error(String.format("Error shutting down container %s-%s", userId, courseId));
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
