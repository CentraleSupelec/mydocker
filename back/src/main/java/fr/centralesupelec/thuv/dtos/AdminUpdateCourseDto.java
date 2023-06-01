package fr.centralesupelec.thuv.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminUpdateCourseDto extends AdminCourseBaseDto {
    @NotEmpty
    @Valid
    private List<SessionUpdateDto> sessions;
}
