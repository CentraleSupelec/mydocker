package fr.centralesupelec.thuv.test_connection_scheduler;

import fr.centralesupelec.gRPC.NodeIPRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.CourseRepository;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NodeIPRequestService {
    private static final Logger logger = LoggerFactory.getLogger(NodeIPRequestService.class);
    private final Map<String, ManagedChannel> channels;
    private final CourseRepository courseRepository;

    public String getNodeIP(String userId, String courseId) {
        String nodeIP = null;
        Optional<Course> course = courseRepository.findById(Long.parseLong(courseId));
        if (course.isPresent()) {
            containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(
                channels.get(course.get().getComputeType().getCluster().getName())
            );
            try {
                nodeIP = stub.getNodeIP(
                       NodeIPRequest.newBuilder()
                               .setCourseID(courseId)
                               .setUserID(userId)
                               .build()
               ).getIPAddress();
                logger.debug("Retrieved IP Address for user '{}' / course '{}' : '{}'", userId, courseId, nodeIP);
           } catch (StatusRuntimeException e) {
               logger.warn(
                       "Could not fetch IP Address for user '{}' / course '{}' ({}) : {}",
                       userId, courseId, e.getStatus().getCode().value(), e.getStatus().getDescription()
               );
           }
        }
        return nodeIP;
    }
}
