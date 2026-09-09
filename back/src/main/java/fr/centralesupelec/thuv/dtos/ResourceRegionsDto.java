package fr.centralesupelec.thuv.dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Data
public class ResourceRegionsDto {
    @NotNull
    private Long ovhResourceId;
    @NotEmpty
    private Set<String> regions;
}
