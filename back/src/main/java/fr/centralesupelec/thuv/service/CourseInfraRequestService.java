package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.CourseInfraRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.CourseRepository;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseInfraRequestService {
    private static final Logger logger = LoggerFactory.getLogger(CourseInfraRequestService.class);
    private final Map<String, ManagedChannel> channels;
    private final CourseRepository courseRepository;

    public Boolean getCourseInfra(String courseId) {
        Boolean nodeWithCourseIdLabelExists = null;
        Optional<Course> course = courseRepository.findById(Long.parseLong(courseId));
        if (course.isPresent()) {
            try {
                containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(
                    channels.get(course.get().getComputeType().getCluster().getName())
                );
                nodeWithCourseIdLabelExists = stub.getCourseInfra(
                        CourseInfraRequest.newBuilder()
                                .setCourseID(courseId)
                                .build()
                ).getNodeWithCourseIdLabelExists();
                 logger.info("Checked if a dedicated node exists for course '{}' : '{}'", courseId, nodeWithCourseIdLabelExists);
            } catch (StatusRuntimeException e) {
                logger.warn(
                        "Could not check if a dedicated node exists for course '{}' / '{}' ({})",
                        courseId, e.getStatus().getCode().value(), e.getStatus().getDescription()
                );
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Cluster unreachable, try again later",
                        e
                );
            }
        }
        return nodeWithCourseIdLabelExists;
    }
}
