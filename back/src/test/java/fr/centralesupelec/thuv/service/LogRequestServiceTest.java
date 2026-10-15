package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.CourseRepository;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogRequestServiceTest {

    private ManagedChannel channel;

    @AfterEach
    void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    void unreachableClusterIsServiceUnavailable() {
        // No server registered under this name: every call fails with UNAVAILABLE.
        channel = InProcessChannelBuilder.forName("absent-" + UUID.randomUUID()).directExecutor().build();
        Cluster cluster = new Cluster();
        cluster.setName("down");
        ComputeType computeType = new ComputeType();
        computeType.setCluster(cluster);
        Course course = new Course();
        course.setComputeType(computeType);
        CourseRepository courseRepository = mock(CourseRepository.class);
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        LogRequestService service = new LogRequestService(Map.of("down", channel), courseRepository);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getLog("3", "7"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
