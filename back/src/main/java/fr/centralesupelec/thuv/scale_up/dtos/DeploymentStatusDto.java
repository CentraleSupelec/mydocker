package fr.centralesupelec.thuv.scale_up.dtos;

import lombok.Data;

@Data
public class DeploymentStatusDto {
    private Long id;
    private String logs;
    private String buildErrors;
    private String status;
    private Long createdOn;
    private Long updatedOn;
}
