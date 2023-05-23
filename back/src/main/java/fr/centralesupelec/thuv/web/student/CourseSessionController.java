package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.thuv.dtos.SessionWithCourseDto;
import fr.centralesupelec.thuv.mappers.SessionWithCourseMapper;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseSessionRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/courses-sessions")
public class CourseSessionController {
    private final UserRepository userRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final SessionWithCourseMapper sessionWithCourseMapper;

    @Autowired
    public CourseSessionController(
            UserRepository userRepository,
            CourseSessionRepository courseSessionRepository,
            SessionWithCourseMapper sessionWithCourseMapper
    ) {
        this.userRepository = userRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.sessionWithCourseMapper = sessionWithCourseMapper;
    }

    @PreAuthorize("hasRole('USER')")
    @RequestMapping(value = "/joined", method = RequestMethod.GET)
    public List<SessionWithCourseDto> getUserCourses(
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        List<CourseSession> listCourseSession = courseSessionRepository
        .findByCourse_UserCourses_UserAndStartDateTimeBeforeAndEndDateTimeAfterAndCourseStatusInOrderByStartDateTime(
                    user,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    Arrays.asList(CourseStatus.DRAFT, CourseStatus.TEST, CourseStatus.READY)
        );
        listCourseSession.addAll(
                courseSessionRepository
                    .findByCourse_UserCourses_UserAndStartDateTimeAfterAndCourseStatusInOrderByStartDateTime(
                            user,
                            LocalDateTime.now(),
                            Arrays.asList(CourseStatus.DRAFT, CourseStatus.TEST, CourseStatus.READY)
                    )
        );
        return listCourseSession
                .stream()
                .map(sessionWithCourseMapper::convertToDto)
                .collect(Collectors.toList());
    }
}
