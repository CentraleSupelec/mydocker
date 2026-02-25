package fr.centralesupelec.thuv.storage;

import fr.centralesupelec.thuv.service.CourseIdsByUserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserCoursesStorageTaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(UserCoursesStorageTaskScheduler.class);
    private final CourseIdsByUserService courseIdsByUserService;
    private final UserCoursesStorage userCoursesStorage;

    @Scheduled(fixedRateString = "${polling_interval_user_courses_in_milliseconds}")
    @Profile("!test")
    public void updateUserCoursesStorage() {
        
        logger.debug("Started updating user active courses. Before : {}", userCoursesStorage.getAllCourses());

        Map<String, List<String>> courseIdsByUserId = courseIdsByUserService.getCourseIdsByUser();
        userCoursesStorage.setAllCourses(courseIdsByUserId);

        logger.debug("Finished updating user active courses. After : {}", userCoursesStorage.getAllCourses());
    }
}
