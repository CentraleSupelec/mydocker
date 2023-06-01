package fr.centralesupelec.thuv.dtos;

import fr.centralesupelec.thuv.scale_up.dtos.CleanDeploymentSummaryDto;
import fr.centralesupelec.thuv.scale_up.dtos.LaunchDeploymentSummaryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SessionDto extends SessionUpdateDto {
    private LaunchDeploymentSummaryDto launchDeployment;
    private CleanDeploymentSummaryDto cleanDeployment;
}
