package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.LogRequest;
import fr.centralesupelec.gRPC.LogResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.CourseRepository;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LogRequestService {
    private final Map<String, ManagedChannel> channels;
    private final CourseRepository courseRepository;

    public LogResponse getLog(String userId, String courseId) {

        Optional<Course> course = courseRepository.findById(Long.parseLong(courseId));
        if (course.isPresent()) {
            containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(
                channels.get(course.get().getComputeType().getCluster().getName())
            );
            try {
                return stub.getLogs(
                        LogRequest.newBuilder()
                                .setCourseID(courseId)
                                .setUserID(userId)
                                .build()
                );
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Cluster unreachable, try again later",
                            e
                    );
                }
                throw e;
            }
        }
        return null;
    }
}
