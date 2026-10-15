package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerStatusRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.ConnectivityState;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.test_connection_scheduler.ContainerTestConnectionTaskScheduler;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Service
public class ContainerStatusConfigureService {
    private static final Logger logger = LoggerFactory.getLogger(ContainerStatusConfigureService.class);

    private final Map<String, StreamObserver<ContainerStatusRequest>> requestObservers = new HashMap<>();
    private final Map<String, ContainerStatusResponseStreamObserver> responseObservers = new HashMap<>();

    private final Map<String, ManagedChannel> channels;
    private final ReentrantLock lock = new ReentrantLock();
    private final CourseRepository courseRepository;
    private final ClusterRepository clusterRepository;

    private final ContainerStorage containerStorage;
    private final ContainerTestConnectionTaskScheduler containerTestConnectionTaskScheduler;
    private Map<String, Boolean> shouldInitializeStubMap = new HashMap<>();

    public void setShouldInitializeStub(String cluster, Boolean shouldInitializeStub) {
        this.shouldInitializeStubMap.put(cluster, shouldInitializeStub);
    }

    @PostConstruct
    public void init() {
        List<Cluster> clusters = clusterRepository.findAll();

        for (Cluster cluster : clusters) {
            shouldInitializeStubMap.put(cluster.getName(), true);
            createNewStubIfNeeded(cluster);
        }
    }

    public void createNewStub(Cluster cluster) {
        String clusterName = cluster.getName();

        ContainerStatusResponseStreamObserver responseObserver =
            new ContainerStatusResponseStreamObserver(containerStorage, containerTestConnectionTaskScheduler, cluster
        );

        containerServiceGrpc.containerServiceStub asyncStub =
            containerServiceGrpc.newStub(channels.get(clusterName));

        responseObserver.setContainerStatusConfigureService(this);

        StreamObserver<ContainerStatusRequest> requestObserver =
            asyncStub.getContainerStatus(responseObserver);

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

    public void configureContainerStatus(String courseID, String userID, ContainerStatusRequest.Action action) {
        ContainerStatusRequest request = ContainerStatusRequest.newBuilder()
                .setCourseID(courseID)
                .setUserID(userID)
                .setAction(action)
                .build();
        try {
            lock.lock();

            Optional<Course> course = courseRepository.findById(Long.parseLong(courseID));
            if (course.isPresent()) {
                Cluster cluster = course.get().getComputeType().getCluster();
                createNewStubIfNeeded(cluster);

                // Get the correct observer for this cluster
                StreamObserver<ContainerStatusRequest> observer =
                    requestObservers.get(cluster.getName());

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
            } else {
                logger.error("Course not found: " + courseID);
            }
        } catch (RuntimeException e) {
            logger.error("Error configuring container status.\n Please restart the go API then this service.");
            throw e;
        } finally {
            lock.unlock();
        }
    }

}
