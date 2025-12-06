package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.SaveDataRequest;
import fr.centralesupelec.gRPC.SaveDataResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
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

    @Autowired
    public SaveDataService(ManagedChannel channel, UserCourseRepository userCourseRepository) {
        this.channel = channel;
        this.userCourseRepository = userCourseRepository;
    }

    @PostConstruct
    public void init() {
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

    public void sendSaveData(SaveDataRequest request) {
        try {
            lock.lock();
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
