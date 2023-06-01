package fr.centralesupelec.thuv.scale_up.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
@EqualsAndHashCode
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LaunchUpdateDeploymentDto.class, name = "launch"),
        @JsonSubTypes.Type(value = CleanUpdateDeploymentDto.class, name = "clean"),
})
public abstract class UpdateDeploymentDto {
    private List<OVHRegionWorkerDto> workers;
    @NotNull
    private Long startDateTime;
    private List<Long> sessions;
    private String description;
}
