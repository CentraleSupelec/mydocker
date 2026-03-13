package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    Optional<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);
    List<UserCourse> findByUserAndCourseStatusInOrderByCourseId(User user, List<CourseStatus> statuses);

    @Query("""
        SELECT uc.course.id, COUNT(uc)
        FROM UserCourse uc
        WHERE uc.lastStartDate >= :dateLimit
        GROUP BY uc.course.id
    """)
    List<Object[]> countRecentUsersByCourse(@Param("dateLimit") LocalDateTime dateLimit);

}
