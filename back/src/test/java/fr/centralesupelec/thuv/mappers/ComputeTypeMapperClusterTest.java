package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.ComputeTypeDto;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.StorageBackend;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import fr.centralesupelec.thuv.scale_up.repository.ResourceRegionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// The deployment forms filter compute types by cluster, so the DTO must carry it.
class ComputeTypeMapperClusterTest {
    private final ComputeTypeMapper mapper = new ComputeTypeMapper(
            mock(OVHRegionRepository.class),
            mock(OVHResourceRepository.class),
            mock(ResourceRegionRepository.class),
            mock(ClusterRepository.class),
            new ClusterMapper());

    private static ComputeType computeType() {
        ComputeType computeType = new ComputeType();
        computeType.setId(4L);
        computeType.setDisplayName("GPU");
        computeType.setTechnicalName("gpu");
        computeType.setStorageBackend(StorageBackend.LOCAL);
        return computeType;
    }

    @Test
    void includesTheComputeTypeCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        cluster.setName("ovh");
        cluster.setGoAddress("10.0.0.2");
        cluster.setGoPort(50000);
        cluster.setDeploymentEnabled(true);
        ComputeType computeType = computeType();
        computeType.setCluster(cluster);

        ComputeTypeDto dto = mapper.convertToComputeTypeDto(computeType);

        assertThat(dto.getCluster()).isNotNull();
        assertThat(dto.getCluster().getId()).isEqualTo(2L);
        assertThat(dto.getCluster().getName()).isEqualTo("ovh");
        assertThat(dto.getCluster().getDeploymentEnabled()).isTrue();
    }

    @Test
    void leavesClusterEmptyWhenTheComputeTypeHasNone() {
        assertThat(mapper.convertToComputeTypeDto(computeType()).getCluster()).isNull();
    }
}
