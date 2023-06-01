package fr.centralesupelec.thuv.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminCourseDto extends AdminCourseBaseDto {
    private Long id;
    private String creator;
    private Long updatedOn;
    private Long createdOn;

    private String link;
    @NotEmpty
    @Valid
    private List<SessionDto> sessions;
}
