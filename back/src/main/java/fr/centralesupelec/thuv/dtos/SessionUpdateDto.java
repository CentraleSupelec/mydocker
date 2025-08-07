package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class SessionUpdateDto {
    private Long id;
    @Pattern(regexp = "^[^/]+$", message = "Title should not contain /")
    private String title;
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
