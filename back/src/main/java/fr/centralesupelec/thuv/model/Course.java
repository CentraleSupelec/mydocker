package fr.centralesupelec.thuv.model;

import static java.lang.Integer.MAX_VALUE;

import fr.centralesupelec.thuv.permissions.models.CoursePermission;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "courses")
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String link;

    @NotBlank
    private String dockerImage;
    private Long nanoCpusLimit;
    private Long memoryBytesLimit;

    @Column(columnDefinition = "INT4 DEFAULT 0", nullable = false)
    private int shutdownAfterMinutes;
    @Column(columnDefinition = "INT4 DEFAULT 0", nullable = false)
    private int warnShutdownMinutes;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "compute_type_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private ComputeType computeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @NotNull
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean saveStudentWork = false;
    private Integer workdirSize;
    private String workdirPath;

    @NotNull
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean allowStudentToSubmit = false;

    @UpdateTimestamp
    private LocalDateTime updatedOn;
    @CreationTimestamp
    private LocalDateTime createdOn;

    @OneToMany(mappedBy = "course")
    @EqualsAndHashCode.Exclude
    private Set<UserCourse> userCourses = new HashSet<>();

    public Set<User> getStudents() {
        return this.userCourses
                .stream()
                .map(UserCourse::getUser)
                .collect(Collectors.toSet())
                ;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User creator;

    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "course")
    @EqualsAndHashCode.Exclude
    private Set<Port> ports = new HashSet<>();

    public Course setPorts(Set<Port> ports) {
        this.ports.clear();
        this.ports.addAll(
                ports.stream()
                    .peek(port -> port.setCourse(this))
                    .collect(Collectors.toSet())
        );
        return this;
    }

    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "course")
    private Set<CourseSession> sessions = new HashSet<>();

    public Course setSessions(Set<CourseSession> courseSessions) {
        this.sessions.clear();
        this.sessions.addAll(
                courseSessions.stream()
                    .peek(courseSession -> courseSession.setCourse(this))
                    .collect(Collectors.toSet())
        );
        return this;
    }

    @Column(length = MAX_VALUE)
    private String displayOptions;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "course")
    @EqualsAndHashCode.Exclude
    private Set<CoursePermission> permissions;
}
