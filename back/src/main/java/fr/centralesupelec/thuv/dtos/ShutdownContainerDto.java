package fr.centralesupelec.thuv.dtos;

import lombok.Data;

@Data
public class ShutdownContainerDto {
    String error;
    Boolean isShutdown = false;
}
