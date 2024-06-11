package fr.centralesupelec.thuv.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ContainerDto {
    private String username;
    private String password;
    private String ip;
    private List<ContainerPortDto> ports;
    private ContainerStatusDto status = ContainerStatusDto.OK;
    private Long deletionTime;
    private Boolean needsNewGpu;
}
