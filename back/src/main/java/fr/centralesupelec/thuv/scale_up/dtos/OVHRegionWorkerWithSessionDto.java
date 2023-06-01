package fr.centralesupelec.thuv.scale_up.dtos;

import fr.centralesupelec.thuv.dtos.SessionWithCourseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OVHRegionWorkerWithSessionDto extends OVHRegionWorkerDto {
    private List<SessionWithCourseDto> sessions = new ArrayList<>();
}
