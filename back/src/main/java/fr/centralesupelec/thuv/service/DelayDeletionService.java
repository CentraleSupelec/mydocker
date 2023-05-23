package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.DelayDeletionRequest;
import fr.centralesupelec.gRPC.DelayDeletionResponse;
import fr.centralesupelec.gRPC.Metadata;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.model.CourseSession;
import io.grpc.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DelayDeletionService {
    private final Channel channel;
    private final ContainerUtilsService containerUtilsService;
    private final ActivityLogger activityLogger;

    public Long delayDeletion(Long userId, CourseSession courseSession) {
        activityLogger.log(
                LogAction.ENVIRONMENT_DELAY_DELETION,
                LogModelName.COURSE,
                courseSession.getCourse().getId().toString()
        );
        containerServiceGrpc.containerServiceBlockingStub blockingStub = containerServiceGrpc
                .newBlockingStub(this.channel);

        Metadata.Builder metadataBuilder = Metadata.newBuilder();
        Long deletionTime = containerUtilsService.computeDeletionTime(courseSession);
        if (deletionTime != null) {
            metadataBuilder.putTags("deleteAfter", "true");
            metadataBuilder.putTags("deletionTime", String.valueOf(deletionTime));
        }

        DelayDeletionRequest request = DelayDeletionRequest
                .newBuilder()
                .setCourseID(String.valueOf(courseSession.getCourse().getId()))
                .setUserID(String.valueOf(userId))
                .setMetadata(metadataBuilder.build())
                .build()
                ;
        DelayDeletionResponse response = blockingStub.delayDeletion(request);
        return response.getDeletionTime();
    }
}
