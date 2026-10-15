package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.ComputeTypeUpdateDto;
import fr.centralesupelec.thuv.dtos.ResourceRegionsDto;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.ResourceRegion;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import fr.centralesupelec.thuv.scale_up.repository.ResourceRegionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Creating a compute type with regions must not query by the unsaved compute type.
class ComputeTypeMapperRegionsTest {
    private final OVHRegionRepository regionRepository = mock(OVHRegionRepository.class);
    private final OVHResourceRepository resourceRepository = mock(OVHResourceRepository.class);
    private final ResourceRegionRepository resourceRegionRepository = mock(ResourceRegionRepository.class);
    private final ComputeTypeMapper mapper = new ComputeTypeMapper(
            regionRepository,
            resourceRepository,
            resourceRegionRepository,
            mock(ClusterRepository.class),
            new ClusterMapper());

    private final OVHResource resource = new OVHResource();
    private final OVHRegion region = new OVHRegion();

    private ComputeTypeUpdateDto dto() {
        when(resourceRepository.findById(9L)).thenReturn(Optional.of(resource));
        when(regionRepository.findById("GRA11")).thenReturn(Optional.of(region));
        ResourceRegionsDto regions = new ResourceRegionsDto();
        regions.setOvhResourceId(9L);
        regions.setRegions(Set.of("GRA11"));
        ComputeTypeUpdateDto dto = new ComputeTypeUpdateDto();
        dto.setDisplayName("GPU");
        dto.setTechnicalName("gpu");
        dto.setStorageBackend("LOCAL");
        dto.setAutoscalingResourcesRegions(List.of(regions));
        return dto;
    }

    @Test
    void newComputeTypeGetsNewRegionsWithoutLookup() {
        ComputeType computeType = new ComputeType();

        mapper.updateComputeType(dto(), computeType);

        verify(resourceRegionRepository, never()).findByComputeTypeAndResourceAndRegion(any(), any(), any());
        assertThat(computeType.getAutoscalingResourcesRegions()).hasSize(1);
        ResourceRegion created = computeType.getAutoscalingResourcesRegions().iterator().next();
        assertThat(created.getComputeType()).isSameAs(computeType);
        assertThat(created.getRegion()).isSameAs(region);
    }

    @Test
    void savedComputeTypeReusesItsExistingRegion() {
        ComputeType computeType = new ComputeType();
        computeType.setId(4L);
        ResourceRegion existing = new ResourceRegion();
        when(resourceRegionRepository.findByComputeTypeAndResourceAndRegion(computeType, resource, region))
                .thenReturn(Optional.of(existing));

        mapper.updateComputeType(dto(), computeType);

        assertThat(computeType.getAutoscalingResourcesRegions()).containsExactly(existing);
    }
}
