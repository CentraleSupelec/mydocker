package fr.centralesupelec.thuv.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserCourseWithSessionDto extends UserCourseDto {
    private List<SessionUpdateDto> sessions;
}
