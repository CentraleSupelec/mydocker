package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A deleted compute type must leave the list the Go API holds: deleting sends the complete list
 * of the cluster again, as an edit does.
 */
class ComputeTypeServiceDeletePushTest {
    private final ComputeTypeRepository computeTypeRepository = mock(ComputeTypeRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final OVHRegionWorkerRepository workerRepository = mock(OVHRegionWorkerRepository.class);
    private final InitAutoscalingService initAutoscalingService = mock(InitAutoscalingService.class);
    private final ComputeTypeService service = new ComputeTypeService(
            computeTypeRepository,
            courseRepository,
            workerRepository,
            mock(ComputeTypeMapper.class),
            initAutoscalingService
    );

    private final Cluster cluster = new Cluster().setName("a").setDeploymentEnabled(true);
    private final ComputeType computeType = new ComputeType().setCluster(cluster);

    @BeforeEach
    void deploymentIsOn() {
        ReflectionTestUtils.setField(service, "deploymentEnabled", true);
        when(computeTypeRepository.countByTechnicalNameAndCluster("", cluster)).thenReturn(1L);
    }

    @Test
    void aDeletedTypeIsFollowedByTheCompleteListOfItsClusterSentToTheGoApi() throws Exception {
        service.deleteComputeType(computeType);

        InOrder order = inOrder(computeTypeRepository, initAutoscalingService);
        order.verify(computeTypeRepository).delete(computeType);
        order.verify(computeTypeRepository).flush();
        order.verify(initAutoscalingService).sendInitRequest(cluster);
    }

    @Test
    void aGoApiThatCannotBeReachedDoesNotFailTheDelete() throws Exception {
        doThrow(new Exception("go api down")).when(initAutoscalingService).sendInitRequest(cluster);

        assertThatCode(() -> service.deleteComputeType(computeType)).doesNotThrowAnyException();

        verify(computeTypeRepository).delete(computeType);
        verify(initAutoscalingService).sendInitRequest(cluster);
    }

    // Guard, green today: nothing is sent when the delete is refused or deployment is off.
    @Test
    void nothingIsSentWhenTheDeleteIsRefusedOrDeploymentIsOff() {
        when(courseRepository.countByComputeType(computeType)).thenReturn(2L);
        assertThatThrownBy(() -> service.deleteComputeType(computeType)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(initAutoscalingService);

        when(courseRepository.countByComputeType(computeType)).thenReturn(0L);
        ReflectionTestUtils.setField(service, "deploymentEnabled", false);
        service.deleteComputeType(computeType);
        verifyNoInteractions(initAutoscalingService);

        ReflectionTestUtils.setField(service, "deploymentEnabled", true);
        cluster.setDeploymentEnabled(false);
        service.deleteComputeType(computeType);
        verifyNoInteractions(initAutoscalingService);
    }
}
