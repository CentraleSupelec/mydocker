package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.AdminContainerRequest;
import fr.centralesupelec.gRPC.AdminContainerResponse;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.mappers.GrpcResponsePortToContainerPortDtoMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RequestAdminContainerService {
    private static final Logger logger = LoggerFactory.getLogger(RequestAdminContainerService.class);

    private final Map<String, ManagedChannel> channels;
    private final ContainerStorage containerStorage;
    // Written from gRPC callback threads as well as request threads: concurrent maps, and stream
    // creation always under the lock, so a request never sees a half-replaced stream.
    private final Map<String, StreamObserver<AdminContainerRequest>> requestObservers = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final GrpcResponsePortToContainerPortDtoMapper grpcResponsePortToContainerPortDtoMapper;
    private final CourseRepository courseRepository;
    private final ClusterRepository clusterRepository;
    private final Map<String, Boolean> shouldInitializeStubMap = new ConcurrentHashMap<>();

    @Autowired
    public RequestAdminContainerService(
        Map<String, ManagedChannel> channels,
            ContainerStorage containerStorage,
            GrpcResponsePortToContainerPortDtoMapper grpcResponsePortToContainerPortDtoMapper,
            CourseRepository courseRepository,
            ClusterRepository clusterRepository
    ) {
        this.channels = channels;
        this.containerStorage = containerStorage;
        this.grpcResponsePortToContainerPortDtoMapper = grpcResponsePortToContainerPortDtoMapper;
        this.courseRepository = courseRepository;
        this.clusterRepository = clusterRepository;
    }

    @PostConstruct
    public void init() {
        List<Cluster> clusters = clusterRepository.findAll();

        for (Cluster cluster : clusters) {
            shouldInitializeStubMap.put(cluster.getName(), true);
            lock.lock();
            try {
                createNewStubIfNeeded(cluster);
            } finally {
                lock.unlock();
            }
        }
    }

    public void createNewStub(Cluster cluster) {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channels.get(cluster.getName()));
        AtomicReference<StreamObserver<AdminContainerRequest>> self = new AtomicReference<>();
        StreamObserver<AdminContainerRequest> requestObserver = asyncStub.getAdminContainer(new StreamObserver<>() {
            public void onNext(AdminContainerResponse adminContainerResponse) {
                ContainerDto containerDto = mapContainerResponseToContainer(adminContainerResponse);
                containerDto.setStatus(ContainerStatusDto.OK);
                containerStorage.addAdminContainer(
                        containerDto,
                        Long.valueOf(adminContainerResponse.getCourseID())
                );
            }
        
            public void onError(Throwable throwable) {
                logger.error("Error requesting a container.\n Please restart the go API then this service.");
                lock.lock();
                try {
                    // Drop the dead stream so requests answer "not ready" until it is recreated.
                    requestObservers.remove(cluster.getName(), self.get());
                    shouldInitializeStubMap.put(cluster.getName(), true);
                } finally {
                    lock.unlock();
                }
                if (logger.isDebugEnabled()) {
                    throwable.printStackTrace();
                }
                Sentry.captureMessage("Unable to obtain the container");
            }
        
            public void onCompleted() {
            }
        });
        self.set(requestObserver);
        requestObservers.put(cluster.getName(), requestObserver);
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
                    lock.lock();
                    try {
                        if (shouldInitializeStubMap.get(cluster.getName())) {
                            createNewStub(cluster);
                            shouldInitializeStubMap.put(cluster.getName(), false);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
    }

    public void requestContainer(AdminContainerRequest request) {
        try {
            lock.lock();
            Optional<Course> course = courseRepository.findById(Long.parseLong(request.getCourseID()));
            if (course.isPresent()) {
                Cluster cluster = course.get().getComputeType().getCluster();
                createNewStubIfNeeded(cluster);

                // The stream of the course's own cluster, never the last one initialized.
                StreamObserver<AdminContainerRequest> observer = requestObservers.get(cluster.getName());

                if (observer != null) {
                    try {
                        observer.onNext(request);
                    } catch (RuntimeException e) {
                        // Cancel RPC
                        observer.onError(e);
                        throw e;
                    }
                } else {
                    // Not sent: say so, never acknowledge a request that did not reach the Go API.
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "The admin container stream to cluster " + cluster.getName() + " is not ready yet, retry in a moment"
                    );
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private ContainerDto mapContainerResponseToContainer(AdminContainerResponse containerResponse) {
        ContainerDto containerDto = new ContainerDto(
            containerResponse.getUserPassword().getUsername(),
            containerResponse.getUserPassword().getPassword(),
            Collections.singletonList(
                grpcResponsePortToContainerPortDtoMapper.convertToContainerPortDto(
                        containerResponse.getPort()
                )
            ),
            LocalDateTime.now()
        );
        containerDto.setIp(containerResponse.getIpAddress());

        // Need to change it if different auth method
        containerDto.setNeedsNewGpu(false);
        return containerDto;
    }
}
