package fr.centralesupelec.thuv.dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Data
public class ComputeTypeUpdateDto {
    @NotNull
    private String displayName;
    @NotNull
    private String technicalName;
    private boolean gpu = false;

    private Set<String> autoscalingRegions;
    private Long autoscalingResource;
    private Long minIdleNodesCount;
    private Long maxNodesCount;
    private Long manualNodesCount;
    @NotEmpty
    private String storageBackend;
}
