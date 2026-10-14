package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.SaveDataRequest;
import fr.centralesupelec.gRPC.SaveDataResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SaveDataService {
    private static final Logger logger = LoggerFactory.getLogger(SaveDataService.class);

    private final Map<String, ManagedChannel> channels;
    private final UserCourseRepository userCourseRepository;
    private StreamObserver<SaveDataRequest> saveDataRequestStreamObserver;
    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, Boolean> shouldInitializeStubMap = new HashMap<>();
    private final CourseRepository courseRepository;
    private final ClusterRepository clusterRepository;

    @Autowired
    public SaveDataService(
        Map<String, ManagedChannel> channels, 
        UserCourseRepository userCourseRepository,
        CourseRepository courseRepository,
        ClusterRepository clusterRepository
    ) {
        this.channels = channels;
        this.userCourseRepository = userCourseRepository;
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

    public void createNewStub(Cluster cluster) {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channels.get(cluster.getName()));
        
        this.saveDataRequestStreamObserver = asyncStub.saveData(new StreamObserver<>() {
            public void onNext(SaveDataResponse saveDataResponse) {
                logger.debug("Received response");
                Optional<UserCourse> optionalUserCourse = userCourseRepository.findByUserIdAndCourseId(
                        Long.parseLong(saveDataResponse.getUserID()),
                        Long.parseLong(saveDataResponse.getCourseID())
                );
                if (!optionalUserCourse.isPresent()) {
                    logger.debug("Could not find usercourse");
                    return;
                }
                UserCourse userCourse = optionalUserCourse.get();
                if (saveDataResponse.getError().equals("")) {
                    logger.debug("No error");
                    userCourse.setSavedAt(new Date());
                    userCourse.setLastSaveError(null);
                } else {
                    logger.debug("Error saving");
                    userCourse.setLastSaveError(saveDataResponse.getError());
                }
                userCourseRepository.saveAndFlush(userCourse);
            }
        
            public void onError(Throwable throwable) {
                logger.error("Stream error");
                shouldInitializeStubMap.put(cluster.getName(), true);
                if (logger.isDebugEnabled()) {
                    throwable.printStackTrace();
                }
                Sentry.captureMessage("Unable to save data");
            }
        
            public void onCompleted() {
                logger.debug("Completed");

            }
        });
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

    public void sendSaveData(SaveDataRequest request) {
        try {
            lock.lock();
            Optional<Course> course = courseRepository.findById(Long.parseLong(request.getCourseID()));
            if (course.isPresent()) {
                createNewStubIfNeeded(course.get().getComputeType().getCluster());
                saveDataRequestStreamObserver.onNext(request);
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            saveDataRequestStreamObserver.onError(e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
