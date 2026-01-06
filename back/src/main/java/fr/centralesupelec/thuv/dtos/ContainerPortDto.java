package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContainerPortDto extends PortDto {
    private Integer portMapTo;
}
