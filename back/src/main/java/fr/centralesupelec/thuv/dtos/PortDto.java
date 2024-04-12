package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class PortDto {
    @NotEmpty
    private String description;
    @NotNull
    private Integer mapPort;
    @NotEmpty
    private String connectionType;
    private Boolean requiredToAccessContainer = false;
    private String hostname;
}
