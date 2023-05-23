package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.CoursePermission;
import fr.centralesupelec.thuv.permissions.models.Permission_;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.domain.Specification;
import fr.centralesupelec.thuv.model.Course_;

import java.util.Collection;

public class CourseSearchSpecifications {
    public static Specification<Course> forUserAndStatusIn(@NotNull User user, Collection<CourseStatus> statuses) {
        return (root, cq, cb) -> {
            Join<Course, CoursePermission> permission = root.join(Course_.PERMISSIONS, JoinType.LEFT);
            cq.where(
                    cb.and(
                        root.get(Course_.STATUS).in(statuses),
                        cb.or(
                                cb.equal(root.get(Course_.CREATOR), user),
                                cb.equal(permission.get(Permission_.USER), user)
                        )
                    )
            );
            return cq.getRestriction();
        };
    }
    public static Specification<Course> forUserAndTitleContainingAndStatusIn(
            @NotNull User user,
            @NotBlank String title,
            Collection<CourseStatus> statuses
    ) {
        return (root, cq, cb) -> {
            Join<Course, CoursePermission> permission = root.join(Course_.PERMISSIONS, JoinType.LEFT);
            cq.where(
                    cb.and(
                            root.get(Course_.STATUS).in(statuses),
                            cb.or(
                                    cb.equal(root.get(Course_.CREATOR), user),
                                    cb.equal(permission.get(Permission_.USER), user)
                            ),
                            cb.like(cb.lower(root.get(Course_.TITLE)), ("%" + title + "%").toLowerCase())
                    )
            );
            return cq.getRestriction();
        };
    }
}
