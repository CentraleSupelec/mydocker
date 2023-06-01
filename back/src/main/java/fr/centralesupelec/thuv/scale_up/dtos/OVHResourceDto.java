package fr.centralesupelec.thuv.scale_up.dtos;

import lombok.Data;

@Data
public class OVHResourceDto {
    private Long id;
    private String type;
    private Long ramInGo;
    private Long coreNumber;
}
