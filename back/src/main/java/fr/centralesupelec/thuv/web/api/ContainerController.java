package fr.centralesupelec.thuv.web.api;

import fr.centralesupelec.gRPC.ContainerRequest;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController("ApiContainerController")
@PreAuthorize("hasRole('BASIC_AUTH')")
@RequestMapping("/container")
@RequiredArgsConstructor
public class ContainerController {
    private final RequestContainerService requestContainerService;
    private final UserCourseRepository userCourseRepository;
    private final ContainerRequestCreatorService containerRequestCreatorService;
    private final ShutdownContainerService shutdownContainerService;
    private final ZoneId zoneId;

    @PostMapping(value = "/{courseId}/{userId}/init")
    public ResponseEntity<Void> initGetContainer(
            @PathVariable("courseId") Course course,
            @PathVariable("userId") User user,
            @RequestParam(defaultValue = "false", required = false) Boolean forceRecreate
    ) {
        userCourseRepository.findByUserIdAndCourseId(
                user.getId(), course.getId()).ifPresent(userCourse -> userCourse.setLastStartDate(LocalDateTime.now(zoneId))
        );
        ContainerRequest containerRequest = containerRequestCreatorService.createRequest(
                course, user, forceRecreate, null, false
        );
        requestContainerService.requestContainer(containerRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping(value = "/{courseId}/{userId}/shutdown")
    public ResponseEntity<Void> shutdownContainer(
            @PathVariable("courseId") long courseId,
            @PathVariable("userId") long userId
    ) {
        shutdownContainerService.shutdownContainer(String.valueOf(userId), String.valueOf(courseId));
        return ResponseEntity.ok().build();
    }
}
