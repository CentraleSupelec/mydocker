package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.SaveDataRequest;
import fr.centralesupelec.gRPC.SaveDataResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.model.UserCourse;
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
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SaveDataService {
    private static final Logger logger = LoggerFactory.getLogger(SaveDataService.class);

    private final ManagedChannel channel;
    private final UserCourseRepository userCourseRepository;
    private StreamObserver<SaveDataRequest> saveDataRequestStreamObserver;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean shouldInitializeStub = true;

    @Autowired
    public SaveDataService(ManagedChannel channel, UserCourseRepository userCourseRepository) {
        this.channel = channel;
        this.userCourseRepository = userCourseRepository;
    }

    @PostConstruct
    public void init() {
        createNewStubIfNeeded();
    }

    public void createNewStub() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);
        
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
                shouldInitializeStub = true;
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

    public void sendSaveData(SaveDataRequest request) {
        try {
            lock.lock();
            createNewStubIfNeeded();
            saveDataRequestStreamObserver.onNext(request);
        } catch (RuntimeException e) {
            // Cancel RPC
            saveDataRequestStreamObserver.onError(e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
