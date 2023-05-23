package fr.centralesupelec.thuv.test_connection_scheduler.dtos;

import fr.centralesupelec.thuv.dtos.ContainerDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
public class ContainerScheduledDto {
    private ContainerDto containerDto;
    @EqualsAndHashCode.Exclude
    private Integer numberOfRetry = 0;
    @EqualsAndHashCode.Exclude
    private LocalDateTime lastExecution;
    private String userId;
    private String courseId;
}
