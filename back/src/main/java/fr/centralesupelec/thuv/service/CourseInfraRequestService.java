package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.CourseInfraRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseInfraRequestService {
    private static final Logger logger = LoggerFactory.getLogger(CourseInfraRequestService.class);
    private final ManagedChannel channel;

    public Boolean getCourseInfra(String courseId) {
        Boolean nodeWithCourseIdLabelExists = null;
        containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(channel);
        try {
            nodeWithCourseIdLabelExists = stub.getCourseInfra(
                    CourseInfraRequest.newBuilder()
                            .setCourseID(courseId)
                            .build()
            ).getNodeWithCourseIdLabelExists();
             logger.info("Checked if a dedicated node exists for course '{}' : '{}'", courseId, nodeWithCourseIdLabelExists);
        } catch (StatusRuntimeException e) {
            logger.warn(
                    "Could not check if a dedicated node exists for course '{}'' / '{}' ({}) : {}",
                    courseId, e.getStatus().getCode().value(), e.getStatus().getDescription()
            );
        }
        return nodeWithCourseIdLabelExists;
    }
}
