package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.gRPC.ContainerRequest;
import fr.centralesupelec.thuv.dtos.DelayDeletionDto;
import fr.centralesupelec.thuv.dtos.ShutdownContainerDto;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.gRPC.Metadata;
import fr.centralesupelec.gRPC.SaveDataRequest;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.mappers.SaveStateMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.service.*;
import fr.centralesupelec.thuv.storage.ShutdownStatusStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/docker")
@RequiredArgsConstructor
public class ContainerController {
    private final ContainerStorage containerStorage;
    private final RequestContainerService requestContainerService;
    private final SaveDataService saveDataService;
    private final UserCourseRepository userCourseRepository;
    private final UserRepository userRepository;
    private final ContainerRequestCreatorService containerRequestCreatorService;
    private final SaveStateMapper saveStateMapper;
    private final LogRequestService logRequestService;
    private final ShutdownContainerService shutdownContainerService;
    private final ShutdownStatusStorage shutdownStatusStorage;
    private final DelayDeletionService delayDeletionService;

    @PostMapping(value = "/initGetContainer/{sessionId}")
    @PreAuthorize("@userCoursePermissionService.canAskContainer(#courseSession)")
    public ResponseEntity<Void> initGetContainer(
            @PathVariable("sessionId") CourseSession courseSession,
            @RequestParam(defaultValue = "false", required = false) Boolean forceRecreate,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        ContainerRequest containerRequest = containerRequestCreatorService.createRequest(
                courseSession, user, forceRecreate
        );
        requestContainerService.requestContainer(containerRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(value = "/saveData/{courseId}", method = RequestMethod.POST)
    @PreAuthorize("@userCoursePermissionService.hasJoin(#course)")
    public ResponseEntity<?> saveData(
            @PathVariable("courseId") Course course,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        if (!course.isAllowStudentToSubmit()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Handle retrieval of the user
        User user = userRepository.getReferenceById(principal.getUserId());
        SaveDataRequest saveDataRequest = SaveDataRequest.newBuilder()
                .setCourseID(String.valueOf(course.getId()))
                .setUserID(String.valueOf(user.getId()))
                .setWorkdirSize(course.getWorkdirSize())
                .setMetadata(
                        Metadata.newBuilder()
                                .putTags("courseId", String.valueOf(course.getId()))
                                .putTags("email", String.valueOf(user.getUsername()))
                                .build()
                )
                .setUserEmail(user.getUsername())
                .build();
        saveDataService.sendSaveData(saveDataRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @RequestMapping(value = "{courseId}/save-state", method = RequestMethod.GET)
    public ResponseEntity<?> getSaveState(
            @PathVariable("courseId") long courseId,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Optional<UserCourse> optionalUserCourse = userCourseRepository
                .findByUserIdAndCourseId(principal.getUserId(), courseId);
        if (optionalUserCourse.isPresent()) {
            return ResponseEntity.ok(
                    saveStateMapper.convertToDto(
                            optionalUserCourse.get()
                    )
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "container/{courseId}", method = RequestMethod.GET)
    public ContainerDto getContainer(
            @PathVariable("courseId") long courseId,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Long userId = principal.getUserId();
        Optional<ContainerDto> optionalContainer = containerStorage.getContainer(
                String.valueOf(userId), String.valueOf(courseId)
        );
        return optionalContainer.orElse(null);
    }

    @GetMapping(value = "logs/{courseId}")
    public String getLogs(
            @PathVariable("courseId") long courseId,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Long userId = principal.getUserId();
        return logRequestService.getLog(
                String.valueOf(userId), String.valueOf(courseId)
        );
    }

    @PostMapping(value = "container/{courseId}/shutdown")
    public void shutdownContainer(
            @PathVariable("courseId") long courseId,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Long userId = principal.getUserId();
        shutdownContainerService.shutdownContainer(String.valueOf(userId), String.valueOf(courseId));
    }

    @PostMapping(value = "/delay-deletion/{sessionId}")
    public DelayDeletionDto delayDeletion(
            @PathVariable("sessionId") CourseSession courseSession,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Long userId = principal.getUserId();
        Long newDeletionTime = delayDeletionService.delayDeletion(userId, courseSession);
        return (new DelayDeletionDto()).setDeletionTime(newDeletionTime);
    }

    @GetMapping(value = "container/{courseId}/shutdown")
    public ShutdownContainerDto getShutdownContainerStatus(
            @PathVariable("courseId") long courseId,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Long userId = principal.getUserId();
        Optional<ShutdownContainerDto> container = shutdownStatusStorage.getContainer(
                String.valueOf(userId), String.valueOf(courseId)
        );
        if (!container.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return container.get();
    }
}
