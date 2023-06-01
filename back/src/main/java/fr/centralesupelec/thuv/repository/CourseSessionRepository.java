package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {
    List<CourseSession> findByCourse_UserCourses_UserAndStartDateTimeAfterAndCourseStatusInOrderByStartDateTime(
            User user, @NotNull LocalDateTime startDateTime, List<CourseStatus> courseStatuses
    );
    List<CourseSession> findByStartDateTimeAfterOrderByStartDateTime(
            @NotNull LocalDateTime startDateTime
    );
    List<CourseSession> findByLaunchDeploymentIsNullAndStartDateTimeAfterOrderByStartDateTime(
            @NotNull LocalDateTime startDateTime
    );
    List<CourseSession> findByCleanDeploymentIsNullAndLaunchDeploymentIsNotNullOrderByStartDateTime();
    List<CourseSession>
    findByCourse_UserCourses_UserAndStartDateTimeBeforeAndEndDateTimeAfterAndCourseStatusInOrderByStartDateTime(
            User user,
            @NotNull LocalDateTime startDateTime,
            @NotNull LocalDateTime endDateTime,
            List<CourseStatus> courseStatuses
    );
}
