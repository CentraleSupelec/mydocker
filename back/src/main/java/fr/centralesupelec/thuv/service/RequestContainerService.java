package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mappers.GrpcResponsePortToContainerPortDtoMapper;
import io.grpc.ConnectivityState;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.storage.ContainerStorage;
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
public class RequestContainerService {
    private static final Logger logger = LoggerFactory.getLogger(RequestContainerService.class);
    private final Map<String, ManagedChannel> channels;

    private final Map<String, StreamObserver<ContainerRequest>> requestObservers = new HashMap<>();
    private final Map<String, ContainerResponseStreamObserver> responseObservers = new HashMap<>();
   
    private final ReentrantLock lock = new ReentrantLock();
    private final ActivityLogger activityLogger;
    private final CourseRepository courseRepository;
    private final ClusterRepository clusterRepository;
    private Map<String, Boolean> shouldInitializeStubMap = new HashMap<>();


    private final GrpcResponsePortToContainerPortDtoMapper grpcResponsePortToContainerPortDtoMapper;
    private final UserCourseRepository userCourseRepository;
    private final ContainerStatusConfigureService containerStatusConfigureService;
    private final ContainerStorage containerStorage;
    private final InitAutoscalingService initAutoscalingService;

    @PostConstruct
    public void init() {
        List<Cluster> clusters = clusterRepository.findAll();

        for (Cluster cluster : clusters) {
            shouldInitializeStubMap.put(cluster.getName(), true);
            createNewStubIfNeeded(cluster);
        }
    }

    public void setShouldInitializeStub(String cluster, Boolean shouldInitializeStub) {
        shouldInitializeStubMap.put(cluster, shouldInitializeStub);
    }

    public void createNewStub(Cluster cluster) {
        String clusterName = cluster.getName();
        
        ContainerResponseStreamObserver responseObserver = 
            new ContainerResponseStreamObserver(
                grpcResponsePortToContainerPortDtoMapper,
                userCourseRepository,
                courseRepository,
                containerStatusConfigureService,
                containerStorage,
                cluster,
                initAutoscalingService
        );
        
        containerServiceGrpc.containerServiceStub asyncStub = 
            containerServiceGrpc.newStub(channels.get(clusterName));

        responseObserver.setRequestContainerService(this);

        StreamObserver<ContainerRequest> requestObserver = 
            asyncStub.getContainer(responseObserver);
        
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
            Optional<Course> course = courseRepository.findById(Long.parseLong(request.getCourseID()));
            if (course.isPresent()) {
                Cluster cluster = course.get().getComputeType().getCluster();
                createNewStubIfNeeded(cluster);

                // Get the correct observer for this cluster
                StreamObserver<ContainerRequest> observer = 
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
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
