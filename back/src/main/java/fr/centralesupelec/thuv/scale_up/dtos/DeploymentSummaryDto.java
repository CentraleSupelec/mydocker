package fr.centralesupelec.thuv.scale_up.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;


@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LaunchDeploymentSummaryDto.class, name = "launch"),
        @JsonSubTypes.Type(value = CleanDeploymentSummaryDto.class, name = "clean"),
})
public abstract class DeploymentSummaryDto {
    private Long id;
    private Long updatedOn;
    private Long startDateTime;
    private String description;
}
