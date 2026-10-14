package fr.centralesupelec.thuv.scale_up.dtos;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import fr.centralesupelec.thuv.dtos.SessionWithCourseDto;
import lombok.Data;

// A session as the deployment form lists it: the usual fields plus the cluster of its course,
// so the form can offer only the sessions of the chosen cluster. Admin endpoints only.
@Data
public class DeploymentSessionDto {
    @JsonUnwrapped
    private SessionWithCourseDto session;
    private Long cluster;
}
