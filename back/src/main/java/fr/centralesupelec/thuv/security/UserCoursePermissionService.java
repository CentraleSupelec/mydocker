package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service("userCoursePermissionService")
public class UserCoursePermissionService {
    private final UserRepository userRepository;

    @Autowired
    public UserCoursePermissionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasJoin(Course course) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return this.hasUserJoin(course, user);
    }

    public boolean canAskContainer(CourseSession courseSession) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        if (courseSession.getBlockContainerCreationBeforeStartTime()) {
            return this.hasUserJoin(courseSession.getCourse(), user)
                    && courseSession.getStartDateTime().isBefore(LocalDateTime.now());
        } else {
            return this.hasUserJoin(courseSession.getCourse(), user);
        }
    }

    private boolean hasUserJoin(Course course, User user) {
        return user.getUserCourses().stream()
                .map(UserCourse::getCourse)
                .map(Course::getId)
                .anyMatch(id -> id.equals(course.getId()));
    }
}
