package fr.centralesupelec.thuv.model;

import fr.centralesupelec.thuv.activity_logging.model.ActivityLogRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.ToString;

import java.util.*;

@Entity
@Table(name = "users", indexes = {
        @Index(columnList = "username", unique = true)
})
@Data
@EqualsAndHashCode(of = {"id"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(columnDefinition = "CITEXT")
    private String email;

    @NotBlank
    @Column(columnDefinition = "CITEXT")
    private String username;

    @NotBlank
    private String name;

    @NotBlank
    private String lastname;

    private Boolean enabled = true;

    public Set<UserCourse> getUserCourses() {
        return userCourses;
    }

    /*
     * Relationships
     */
    @ToString.Exclude
    @OneToMany(
        cascade = { CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE },
        fetch = FetchType.LAZY,
        mappedBy = "user",
        orphanRemoval = true
    )
    private Set<UserCourse> userCourses = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"
            )
    )
    private Collection<Role> roles = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "creator",
        cascade = { CascadeType.REMOVE }
    )
    private Set<Course> createdCourses = new HashSet<>();

    public void addCourse(Course course) {
        Optional<UserCourse> userCourseOptional = this.userCourses
                .stream()
                .filter(userCourse -> userCourse.getCourse().equals(course))
                .findFirst();
        if (userCourseOptional.isPresent()) {
            return;
        }
        UserCourse userCourse = new UserCourse();
        userCourse.setCourse(course);
        this.addUserCourse(userCourse);
    }

    public void removeCourse(Course course) {
        Optional<UserCourse> userCourseOptional = this.userCourses
                .stream()
                .filter(userCourse -> userCourse.getCourse().getId().equals(course.getId()))
                .findAny();
        if (userCourseOptional.isPresent()) {
            userCourseOptional.get().setUser(null);
            this.removeUserCourse(userCourseOptional.get());
        }
    }

    public void addUserCourse(UserCourse userCourse) {
        this.userCourses.add(userCourse);
        userCourse.setUser(this);
    }

    public void removeUserCourse(UserCourse userCourse) {
        this.userCourses.remove(userCourse);
        userCourse.setUser(null);
    }

    @ToString.Exclude
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ActivityLogRecord> logs = new ArrayList<>();

    @PreRemove
    public void removeUserFromLogs() {
        for (ActivityLogRecord log: logs) {
            log.setUser(null);
        }
    }
}
