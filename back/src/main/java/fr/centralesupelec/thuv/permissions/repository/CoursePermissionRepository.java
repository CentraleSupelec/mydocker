package fr.centralesupelec.thuv.permissions.repository;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.CoursePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

public interface CoursePermissionRepository extends JpaRepository<CoursePermission, Long> {
    Optional<CoursePermission> findByUserAndCourse(@NotNull User user, @NotNull Course course);
    List<CoursePermission> findByUser(@NotNull User user);
    List<CoursePermission> findByCourse(@NotNull Course course);
}
