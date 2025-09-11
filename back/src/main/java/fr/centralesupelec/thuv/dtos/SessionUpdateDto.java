package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class SessionUpdateDto {
    private Long id;
    @NotNull
    private Long startDateTime;
    @NotNull
    private Long endDateTime;
    @NotNull
    private Boolean blockContainerCreationBeforeStartTime;
    @NotNull
    private Boolean destroyContainerAfterEndTime;
    @NotNull
    private Long studentNumber;
}
