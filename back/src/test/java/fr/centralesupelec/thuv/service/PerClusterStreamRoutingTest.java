package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.AdminContainerRequest;
import fr.centralesupelec.gRPC.SaveDataRequest;
import fr.centralesupelec.thuv.mappers.GrpcResponsePortToContainerPortDtoMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import io.grpc.ClientCall;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Two connected clusters: a request for a course must go out on its own cluster's stream,
// whichever cluster initialized last.
class PerClusterStreamRoutingTest {
    private final ManagedChannel channelA = mock(ManagedChannel.class);
    private final ManagedChannel channelB = mock(ManagedChannel.class);
    @SuppressWarnings("unchecked")
    private final ClientCall<Object, Object> callA = mock(ClientCall.class);
    @SuppressWarnings("unchecked")
    private final ClientCall<Object, Object> callB = mock(ClientCall.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);

    @BeforeEach
    void setUp() {
        Cluster clusterA = cluster(1L, "a");
        Cluster clusterB = cluster(2L, "b");
        when(clusterRepository.findAll()).thenReturn(List.of(clusterA, clusterB));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(clusterA)));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(course(clusterB)));
        connect(channelA, callA);
        connect(channelB, callB);
    }

    @Test
    void saveDataGoesToTheCourseCluster() {
        SaveDataService service = new SaveDataService(
                Map.of("a", channelA, "b", channelB),
                mock(UserCourseRepository.class),
                courseRepository,
                clusterRepository
        );
        service.init();

        SaveDataRequest forA = SaveDataRequest.newBuilder().setCourseID("10").build();
        service.sendSaveData(forA);
        SaveDataRequest forB = SaveDataRequest.newBuilder().setCourseID("20").build();
        service.sendSaveData(forB);

        verify(callA).sendMessage(forA);
        verify(callB, never()).sendMessage(forA);
        verify(callB).sendMessage(forB);
        verify(callA, never()).sendMessage(forB);
    }

    @Test
    void adminContainerGoesToTheCourseCluster() {
        RequestAdminContainerService service = new RequestAdminContainerService(
                Map.of("a", channelA, "b", channelB),
                mock(ContainerStorage.class),
                mock(GrpcResponsePortToContainerPortDtoMapper.class),
                courseRepository,
                clusterRepository
        );
        service.init();

        AdminContainerRequest forA = AdminContainerRequest.newBuilder().setCourseID("10").build();
        service.requestContainer(forA);

        verify(callA).sendMessage(forA);
        verify(callB, never()).sendMessage(forA);
    }

    private static void connect(ManagedChannel channel, ClientCall<Object, Object> call) {
        when(channel.getState(true)).thenReturn(ConnectivityState.READY);
        when(channel.newCall(any(), any())).thenReturn(call);
    }

    private static Cluster cluster(Long id, String name) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        cluster.setName(name);
        return cluster;
    }

    private static Course course(Cluster cluster) {
        return new Course().setComputeType(new ComputeType().setCluster(cluster));
    }
}
