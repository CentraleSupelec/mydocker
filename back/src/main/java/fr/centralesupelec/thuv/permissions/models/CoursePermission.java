package fr.centralesupelec.thuv.permissions.models;

import fr.centralesupelec.thuv.model.Course;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@DiscriminatorValue("COURSE")
@EqualsAndHashCode(callSuper = true)
@Data
public class CoursePermission extends Permission {
    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    @JoinColumn(name = "course_id", nullable = true)
    private Course course;
}
