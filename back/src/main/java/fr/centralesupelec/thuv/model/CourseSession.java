package fr.centralesupelec.thuv.model;

import fr.centralesupelec.thuv.scale_up.model.CleanDeployment;
import fr.centralesupelec.thuv.scale_up.model.CourseSessionOVHResource;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_sessions")
@Data
public class CourseSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDateTime startDateTime;

    @NotNull
    private LocalDateTime endDateTime;

    @NotNull
    private Boolean blockContainerCreationBeforeStartTime;

    @NotNull
    private Boolean destroyContainerAfterEndTime;

    @ManyToOne
    @JoinColumn(nullable = false)
    @EqualsAndHashCode.Exclude
    private Course course;

    @NotNull
    private Long studentNumber;

    @OneToMany(
            mappedBy = "courseSession",
            fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true
    )
    @EqualsAndHashCode.Exclude
    private Set<CourseSessionOVHResource> courseSessionOvhResources = new HashSet<>();

    @ManyToOne
    @JoinColumn()
    @EqualsAndHashCode.Exclude
    private LaunchDeployment launchDeployment;

    @ManyToOne
    @JoinColumn()
    @EqualsAndHashCode.Exclude
    private CleanDeployment cleanDeployment;
}
