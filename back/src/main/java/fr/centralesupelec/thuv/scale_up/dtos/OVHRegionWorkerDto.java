package fr.centralesupelec.thuv.scale_up.dtos;

import lombok.Data;

@Data
public class OVHRegionWorkerDto {
    private Long id;
    private String region;
    private Long resource;
    private Long count;
    private Long computeTypeId;
}
