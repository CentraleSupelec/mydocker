package fr.centralesupelec.thuv.scale_up.deployment_scheduler;

import lombok.Data;

@Data
public class GrpcWorkerDto {
    private Long count;
    private String flavor;
    private String imageId;
    private String region;
    private String owner;
}
