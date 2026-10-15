package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComputeTypeServiceDeleteTest {
    private final ComputeTypeRepository computeTypeRepository = mock(ComputeTypeRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final OVHRegionWorkerRepository workerRepository = mock(OVHRegionWorkerRepository.class);
    private final ComputeTypeService service = new ComputeTypeService(
            computeTypeRepository,
            courseRepository,
            workerRepository,
            mock(ComputeTypeMapper.class),
            mock(InitAutoscalingService.class)
    );

    private final ComputeType computeType = new ComputeType().setCluster(new Cluster());

    private static void assertConflict(Throwable e) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void refusesToDeleteAComputeTypeUsedByACourse() {
        when(courseRepository.countByComputeType(computeType)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteComputeType(computeType))
                .satisfies(ComputeTypeServiceDeleteTest::assertConflict)
                .hasMessageContaining("used by 2 course(s)");
        verify(computeTypeRepository, never()).delete(computeType);
    }

    @Test
    void refusesToDeleteAComputeTypeUsedByAWorker() {
        when(workerRepository.countByComputeType(computeType)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteComputeType(computeType))
                .satisfies(ComputeTypeServiceDeleteTest::assertConflict)
                .hasMessageContaining("used by 3 deployment worker(s)");
        verify(computeTypeRepository, never()).delete(computeType);
    }

    // A reference added between the counts and the delete is caught by the foreign key.
    @Test
    void aForeignKeyViolationIsAConflict() {
        doThrow(new DataIntegrityViolationException("fk")).when(computeTypeRepository).flush();

        assertThatThrownBy(() -> service.deleteComputeType(computeType))
                .satisfies(ComputeTypeServiceDeleteTest::assertConflict)
                .hasMessageContaining("still in use");
    }

    @Test
    void deletesAnUnusedComputeType() {
        when(computeTypeRepository.countByTechnicalNameAndCluster("", computeType.getCluster())).thenReturn(1L);

        service.deleteComputeType(computeType);

        verify(computeTypeRepository).delete(computeType);
    }
}
