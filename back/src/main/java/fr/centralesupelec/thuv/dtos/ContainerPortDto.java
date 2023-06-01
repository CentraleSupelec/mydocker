package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerPortDto extends PortDto {
    private Integer portMapTo;
}
