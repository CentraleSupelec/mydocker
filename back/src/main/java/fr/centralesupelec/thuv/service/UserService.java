package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.UserCourse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Primary
@Service
@RequiredArgsConstructor
public class UserService {
    private final ZoneId zoneId;

    public void addCourseToUser(Course course, User user) {
        Optional<UserCourse> userCourseOptional = user.getUserCourses()
                .stream()
                .filter(userCourse -> userCourse.getCourse().equals(course))
                .findFirst();
        if (userCourseOptional.isPresent()) {
            userCourseOptional.get().setLastStartDate(LocalDateTime.now(zoneId));
            return;
        }
        UserCourse userCourse = new UserCourse();
        userCourse.setCourse(course).setLastStartDate(LocalDateTime.now(zoneId));
        user.addUserCourse(userCourse);
    }
}
