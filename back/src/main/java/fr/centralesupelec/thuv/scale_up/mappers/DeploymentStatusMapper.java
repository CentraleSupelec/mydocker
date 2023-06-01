package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.scale_up.dtos.DeploymentStatusDto;
import fr.centralesupelec.thuv.scale_up.model.DeploymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DeploymentStatusMapper {
    private final ZoneId zoneId;

    public DeploymentStatusDto convertToDto(DeploymentStatus deploymentStatus) {
        return new DeploymentStatusDto()
                .setId(deploymentStatus.getId())
                .setLogs(deploymentStatus.getLogs())
                .setStatus(deploymentStatus.getStatus().name())
                .setBuildErrors(deploymentStatus.getBuildErrors())
                .setCreatedOn(
                        deploymentStatus.getCreatedOn().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setUpdatedOn(
                        deploymentStatus.getUpdatedOn().atZone(zoneId).toInstant().toEpochMilli()
                );
    }

}
