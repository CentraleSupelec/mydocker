package fr.centralesupelec.thuv.scale_up.dtos;

import fr.centralesupelec.thuv.dtos.AdminCourseDto;
import fr.centralesupelec.thuv.dtos.SessionUpdateDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourseSessionWithResourceDto extends SessionUpdateDto {
    private AdminCourseDto course;
    @NotEmpty
    private List<ResourceDescriptionDto> resources;
}
