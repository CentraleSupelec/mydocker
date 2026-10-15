package fr.centralesupelec.thuv.dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ClusterDto {
    @NotNull
    private Long id;
    @NotEmpty
    private String name;
    @NotEmpty
    private String goAddress;
    @NotNull
    private int goPort;
    private String terraformStateUrl;
    @NotNull
    private Boolean deploymentEnabled;
}
