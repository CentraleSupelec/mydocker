package fr.centralesupelec.thuv.scale_up.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ResourceDescriptionDto {
    @NotNull
    private Long ovhResourceId;
    @NotNull
    private Long count;
}
