package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.CourseSearchSpecifications;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseListService {
    private final CourseRepository courseRepository;

    /**
     * hasAdminAuthority comes from the session, not from the user row: an account may hold
     * ROLE_ADMIN in the database while the session it is using was opened through a provider that
     * does not grant administrator authority.
     * Deleted courses are listed for administrators only; the status list comes straight from the
     * client, so the front's filter is not a guard.
     */
    public Page<Course> getViewableCourse(
            User user, boolean hasAdminAuthority, String search, List<CourseStatus> status, Pageable pageable
    ) {
        if (hasAdminAuthority) {
            return courseRepository.searchCoursesByTitleOrCreatorName(search, status, pageable);
        }

        if (status != null && status.contains(CourseStatus.DELETED)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deleted courses are listed for administrators only");
        }

        if (StringUtils.isEmpty(search)) {
            return courseRepository.findAll(CourseSearchSpecifications.forUserAndStatusIn(user, status), pageable);
        }
        return courseRepository.findAll(CourseSearchSpecifications.forUserAndTitleContainingAndStatusIn(
                user, search, status
        ), pageable);
    }
}
