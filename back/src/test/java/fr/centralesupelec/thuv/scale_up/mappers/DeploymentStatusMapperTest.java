package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.scale_up.dtos.DeploymentStatusDto;
import fr.centralesupelec.thuv.scale_up.model.DeploymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentStatusMapperTest {
    private final DeploymentStatusMapper mapper = new DeploymentStatusMapper(ZoneId.of("UTC"));

    @Test
    void includesClusterIdAndPreservesExistingFields() {
        Cluster cluster = new Cluster();
        cluster.setId(42L);
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 7, 12, 0);
        DeploymentStatus status = new DeploymentStatus()
                .setId(7L)
                .setCluster(cluster)
                .setLogs("deployment logs")
                .setBuildErrors("build errors")
                .setStatus(DeploymentStatus.Status.ERROR)
                .setCreatedOn(timestamp)
                .setUpdatedOn(timestamp);

        DeploymentStatusDto dto = mapper.convertToDto(status);

        assertThat(dto.getCluster()).isEqualTo(42L);
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getLogs()).isEqualTo("deployment logs");
        assertThat(dto.getBuildErrors()).isEqualTo("build errors");
        assertThat(dto.getStatus()).isEqualTo("ERROR");
        assertThat(dto.getCreatedOn()).isEqualTo(timestamp.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
        assertThat(dto.getUpdatedOn()).isEqualTo(dto.getCreatedOn());
    }

    @Test
    void supportsExistingStatusesWithoutCluster() {
        DeploymentStatus status = new DeploymentStatus()
                .setCreatedOn(LocalDateTime.of(2026, 10, 7, 12, 0))
                .setUpdatedOn(LocalDateTime.of(2026, 10, 7, 12, 0));

        assertThat(mapper.convertToDto(status).getCluster()).isNull();
    }
}
