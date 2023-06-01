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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseListService {
    private final CourseRepository courseRepository;

    public Page<Course> getViewableCourse(User user, String search, List<CourseStatus> status, Pageable pageable) {
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            return courseRepository.findAllByTitleContainingIgnoreCaseAndStatusIn(search, status, pageable);
        }

        if (StringUtils.isEmpty(search)) {
            return courseRepository.findAll(CourseSearchSpecifications.forUserAndStatusIn(user, status), pageable);
        }
        return courseRepository.findAll(CourseSearchSpecifications.forUserAndTitleContainingAndStatusIn(
                user, search, status
        ), pageable);
    }
}
