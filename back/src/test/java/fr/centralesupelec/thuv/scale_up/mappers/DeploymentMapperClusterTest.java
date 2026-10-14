package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.mappers.SessionWithCourseMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseSessionRepository;
import fr.centralesupelec.thuv.scale_up.dtos.CleanUpdateDeploymentDto;
import fr.centralesupelec.thuv.scale_up.dtos.LaunchUpdateDeploymentDto;
import fr.centralesupelec.thuv.scale_up.dtos.OVHRegionWorkerDto;
import fr.centralesupelec.thuv.scale_up.dtos.UpdateDeploymentDto;
import fr.centralesupelec.thuv.scale_up.model.CleanDeployment;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeploymentMapperClusterTest {
    private final CourseSessionRepository courseSessionRepository = mock(CourseSessionRepository.class);
    private final OVHRegionWorkerRepository ovhRegionWorkerRepository = mock(OVHRegionWorkerRepository.class);
    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);
    private final ComputeTypeRepository computeTypeRepository = mock(ComputeTypeRepository.class);
    private final OVHRegionWorkerMapper ovhRegionWorkerMapper = mock(OVHRegionWorkerMapper.class);
    private final DeploymentMapper mapper = new DeploymentMapper(
            ZoneId.of("UTC"),
            ovhRegionWorkerMapper,
            mock(OVHRegionWorkerWithSessionMapper.class),
            courseSessionRepository,
            ovhRegionWorkerRepository,
            mock(SessionWithCourseMapper.class),
            clusterRepository,
            computeTypeRepository
    );

    private final Cluster clusterA = cluster(1L);
    private final Cluster clusterB = cluster(2L);

    @BeforeEach
    void setUp() {
        when(clusterRepository.getReferenceById(1L)).thenReturn(clusterA);
        when(courseSessionRepository.getReferenceById(10L)).thenReturn(session(10L, clusterA));
        when(courseSessionRepository.getReferenceById(20L)).thenReturn(session(20L, clusterB));
        when(courseSessionRepository.getReferenceById(30L)).thenReturn(session(30L, null));
        when(computeTypeRepository.getReferenceById(5L)).thenReturn(new ComputeType().setCluster(clusterA));
        when(ovhRegionWorkerRepository.getReferenceById(200L))
                .thenReturn(new OVHRegionWorker().setComputeType(new ComputeType().setCluster(clusterB)));
    }

    @Test
    void launchAcceptsSessionsOfTheDeploymentCluster() throws Exception {
        LaunchDeployment deployment = new LaunchDeployment();

        mapper.applyChange(deployment, dto(new LaunchUpdateDeploymentDto(), List.of(10L, 30L), List.of()));

        // Size only: the session and deployment toString call each other.
        assertThat(deployment.getSessionsToLaunch().size()).isEqualTo(2);
    }

    @Test
    void launchRejectsASessionOfAnotherCluster() {
        assertThatThrownBy(() -> mapper.applyChange(
                new LaunchDeployment(),
                dto(new LaunchUpdateDeploymentDto(), List.of(10L, 20L), List.of())
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Session 20");
    }

    @Test
    void cleanRejectsASessionOfAnotherCluster() {
        assertThatThrownBy(() -> mapper.applyChange(
                new CleanDeployment(),
                dto(new CleanUpdateDeploymentDto(), List.of(20L), List.of())
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Session 20");
    }

    @Test
    void launchAcceptsAWorkerWithoutComputeType() throws Exception {
        OVHRegionWorkerDto ownerless = new OVHRegionWorkerDto().setRegion("GRA").setCount(1L);
        when(ovhRegionWorkerMapper.convertToModel(ownerless)).thenReturn(new OVHRegionWorker().setId(1L));
        LaunchDeployment deployment = new LaunchDeployment();

        mapper.applyChange(deployment, dto(new LaunchUpdateDeploymentDto(), List.of(10L), List.of(ownerless)));

        assertThat(deployment.getWorkersToLaunch().size()).isEqualTo(1);
    }

    @Test
    void launchRejectsAComputeTypeOfAnotherCluster() {
        when(computeTypeRepository.getReferenceById(6L)).thenReturn(new ComputeType().setCluster(clusterB));
        OVHRegionWorkerDto worker = new OVHRegionWorkerDto().setComputeTypeId(6L);

        assertThatThrownBy(() -> mapper.applyChange(
                new LaunchDeployment(),
                dto(new LaunchUpdateDeploymentDto(), List.of(10L), List.of(worker))
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Compute type 6");
    }

    // The request names a compute type of the deployment cluster; the stored worker runs elsewhere.
    @Test
    void cleanRejectsAWorkerOfAnotherCluster() {
        OVHRegionWorkerDto worker = new OVHRegionWorkerDto().setId(200L).setComputeTypeId(5L);
        assertThatThrownBy(() -> mapper.applyChange(
                new CleanDeployment(),
                dto(new CleanUpdateDeploymentDto(), List.of(10L), List.of(worker))
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Worker 200");
    }

    @Test
    void cleanRejectsAnOwnerlessWorkerLaunchedOnAnotherCluster() {
        LaunchDeployment launchedOnB = new LaunchDeployment();
        launchedOnB.setCluster(clusterB);
        when(ovhRegionWorkerRepository.getReferenceById(300L))
                .thenReturn(new OVHRegionWorker().setLaunchDeployment(launchedOnB));

        assertThatThrownBy(() -> mapper.applyChange(
                new CleanDeployment(),
                dto(new CleanUpdateDeploymentDto(), List.of(10L), List.of(new OVHRegionWorkerDto().setId(300L)))
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Worker 300");
    }

    @Test
    void cleanAcceptsAnOwnerlessWorkerLaunchedOnTheSameCluster() throws Exception {
        LaunchDeployment launchedOnA = new LaunchDeployment();
        launchedOnA.setCluster(clusterA);
        when(ovhRegionWorkerRepository.getReferenceById(301L))
                .thenReturn(new OVHRegionWorker().setId(301L).setLaunchDeployment(launchedOnA));
        CleanDeployment deployment = new CleanDeployment();

        mapper.applyChange(deployment, dto(new CleanUpdateDeploymentDto(), List.of(10L), List.of(new OVHRegionWorkerDto().setId(301L))));

        assertThat(deployment.getWorkersToClean().size()).isEqualTo(1);
    }

    private static UpdateDeploymentDto dto(UpdateDeploymentDto dto, List<Long> sessions, List<OVHRegionWorkerDto> workers) {
        dto.setCluster(1L);
        dto.setStartDateTime(0L);
        dto.setSessions(sessions);
        dto.setWorkers(workers);
        return dto;
    }

    private static Cluster cluster(Long id) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        return cluster;
    }

    private static CourseSession session(Long id, Cluster cluster) {
        return new CourseSession().setId(id).setCourse(new Course().setComputeType(new ComputeType().setCluster(cluster)));
    }
}
