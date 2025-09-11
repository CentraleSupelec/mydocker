package fr.centralesupelec.thuv.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "users_courses")
@Data
public class UserCourse implements Serializable {
    @EmbeddedId
    private UserCoursePK id = new UserCoursePK();

    private Date savedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastStartDate;

    private String lastSaveError;

    private String username;

    private String password;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;
}
