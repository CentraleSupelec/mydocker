package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.thuv.dtos.UserCourseDto;
import fr.centralesupelec.thuv.dtos.UserCourseWithSessionDto;
import fr.centralesupelec.thuv.mappers.UserCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/courses")
public class CourseController {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseMapper userCourseMapper;

    @Autowired
    public CourseController(
            UserRepository userRepository,
            CourseRepository courseRepository,
            UserCourseMapper userCourseMapper
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.userCourseMapper = userCourseMapper;
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/joined", method = RequestMethod.GET)
    public List<UserCourseWithSessionDto> getUserCourses(
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        List<Course> listCourse = courseRepository.findByUserCoursesUserAndStatusInOrderById(
                user, Arrays.asList(CourseStatus.DRAFT, CourseStatus.TEST, CourseStatus.READY)
        );
        return listCourse.stream()
                .map(userCourseMapper::convertToDtoWihSession)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/{courseLink}", method = RequestMethod.GET)
    public ResponseEntity<UserCourseDto> getCourseInformations(
        @PathVariable("courseLink") String courseLink
    ) {
        Course course = courseRepository.getCourseByLink(courseLink);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(
                userCourseMapper.convertToDto(course)
        );
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/{courseId}/join", method = RequestMethod.PUT)
    public UserCourseDto joinCourse(
            @PathVariable("courseId") Course course,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        user.addCourse(course);
        userRepository.save(user);
        return userCourseMapper.convertToDto(course);
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/{courseId}/quit", method = RequestMethod.PUT)
    public List<UserCourseDto> quitCourse(
        @PathVariable("courseId") Course course,
        @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        user.removeCourse(course);
        User updatedUser = userRepository.saveAndFlush(user);
        List<UserCourseDto> listCourses = new ArrayList<>();
        for (UserCourse i : updatedUser.getUserCourses()) {
            listCourses.add(
                    userCourseMapper.convertToDto(i.getCourse())
            );
        }
        return listCourses;
    }
}
