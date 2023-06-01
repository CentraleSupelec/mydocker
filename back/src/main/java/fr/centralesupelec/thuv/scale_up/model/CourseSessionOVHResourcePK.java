package fr.centralesupelec.thuv.scale_up.model;

import lombok.Data;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
@Data
public class CourseSessionOVHResourcePK implements Serializable {
    @Column(name = "course_session_id")
    private Long courseSessionId;
    @Column(name = "ovh_ressource_id")
    private Long ovhRessourceId;
}
