package fr.centralesupelec.thuv.scale_up.model;

import fr.centralesupelec.thuv.model.CourseSession;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "course_sessions__ovh_ressource")
@Data
public class CourseSessionOVHResource {
    @EmbeddedId
    private CourseSessionOVHResourcePK id = new CourseSessionOVHResourcePK();

    @NotNull
    @EqualsAndHashCode.Exclude
    private Long count;

    @ManyToOne
    @MapsId("courseSessionId")
    @JoinColumn(name = "course_session_id")
    private CourseSession courseSession;

    @ManyToOne
    @MapsId("ovhRessourceId")
    @JoinColumn(name = "ovh_ressource_id")
    private OVHResource ovhResource;

    public CourseSessionOVHResource setCourseSession(CourseSession courseSession) {
        this.courseSession = courseSession;
        this.id.setCourseSessionId(
                courseSession.getId()
        );
        return this;
    }

    public CourseSessionOVHResource setOvhResource(OVHResource ovhResource) {
        this.ovhResource = ovhResource;
        this.id.setOvhRessourceId(
                ovhResource.getId()
        );
        return this;
    }
}
