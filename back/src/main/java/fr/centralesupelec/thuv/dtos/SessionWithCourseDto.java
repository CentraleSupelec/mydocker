package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SessionWithCourseDto extends SessionUpdateDto {
    private UserCourseDto course;
}
