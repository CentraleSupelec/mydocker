package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComputeTypeServiceDeleteTest {
    private final ComputeTypeRepository computeTypeRepository = mock(ComputeTypeRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final ComputeTypeService service = new ComputeTypeService(
            computeTypeRepository,
            courseRepository,
            mock(ComputeTypeMapper.class),
            mock(InitAutoscalingService.class)
    );

    private final ComputeType computeType = new ComputeType().setCluster(new Cluster());

    @Test
    void refusesToDeleteAComputeTypeUsedByACourse() {
        when(courseRepository.countByComputeType(computeType)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteComputeType(computeType))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("used by 2 course(s)");
        verify(computeTypeRepository, never()).delete(computeType);
    }

    @Test
    void deletesAnUnusedComputeType() {
        when(courseRepository.countByComputeType(computeType)).thenReturn(0L);
        when(computeTypeRepository.countByTechnicalNameAndCluster("", computeType.getCluster())).thenReturn(1L);

        service.deleteComputeType(computeType);

        verify(computeTypeRepository).delete(computeType);
    }
}
