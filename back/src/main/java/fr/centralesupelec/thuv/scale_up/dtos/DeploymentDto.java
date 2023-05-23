package fr.centralesupelec.thuv.scale_up.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fr.centralesupelec.thuv.dtos.SessionWithCourseDto;
import lombok.Data;

import java.util.List;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LaunchDeploymentDto.class, name = "launch"),
        @JsonSubTypes.Type(value = CleanDeploymentDto.class, name = "clean"),
})
public abstract class DeploymentDto extends DeploymentSummaryDto {
    private List<OVHRegionWorkerWithSessionDto> workers;
    private List<SessionWithCourseDto> sessions;
}
