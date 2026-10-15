package fr.centralesupelec.thuv.scale_up;

import fr.centralesupelec.thuv.dtos.SessionWithCourseDto;
import fr.centralesupelec.thuv.mappers.SessionWithCourseMapper;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.repository.CourseSessionRepository;
import fr.centralesupelec.thuv.scale_up.dtos.DeploymentDto;
import fr.centralesupelec.thuv.scale_up.dtos.LaunchUpdateDeploymentDto;
import fr.centralesupelec.thuv.scale_up.dtos.OVHRegionWorkerWithSessionDto;
import fr.centralesupelec.thuv.scale_up.dtos.UpdateDeploymentDto;
import fr.centralesupelec.thuv.scale_up.mappers.DeploymentMapper;
import fr.centralesupelec.thuv.scale_up.mappers.OVHRegionWorkerWithSessionMapper;
import fr.centralesupelec.thuv.scale_up.model.CleanDeployment;
import fr.centralesupelec.thuv.scale_up.model.Deployment;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import fr.centralesupelec.thuv.scale_up.repository.DeploymentRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import fr.centralesupelec.thuv.scale_up.services.DeleteDeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/deployment")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DeploymentController {
    private final DeploymentRepository deploymentRepository;
    private final DeploymentMapper deploymentMapper;
    private final SessionWithCourseMapper sessionWithCourseMapper;
    private final CourseSessionRepository courseSessionRepository;
    private final ZoneId zoneId;
    private final OVHRegionWorkerRepository ovhRegionWorkerRepository;
    private final OVHRegionWorkerWithSessionMapper ovhRegionWorkerWithSessionMapper;
    private final DeleteDeploymentService deleteDeploymentService;

    @GetMapping
    public List<DeploymentDto> getDeployments() {
        return deploymentRepository.findByOrderByStartDateTimeDesc()
                .stream()
                .map(deploymentMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("{id}")
    public DeploymentDto getDeployment(
            @PathVariable("id") Deployment deployment
    ) {
        return deploymentMapper.convertToDto(deployment);
    }

    @PostMapping
    public void createDeployment(
            @RequestBody @Valid UpdateDeploymentDto updateDeploymentDto
    ) {
        Deployment deployment;
        if (updateDeploymentDto instanceof LaunchUpdateDeploymentDto) {
            deployment = new LaunchDeployment();
        } else {
            deployment = new CleanDeployment();
        }
        deploymentMapper.applyChange(deployment, updateDeploymentDto);
        deploymentRepository.save(deployment);
    }

    @PutMapping(value = "{id}")
    public void updateDeployment(
            @PathVariable("id") Deployment deployment,
            @RequestBody @Valid UpdateDeploymentDto updateDeploymentDto
    ) {
        deploymentMapper.applyChange(deployment, updateDeploymentDto);
        deploymentRepository.save(deployment);
    }

    @DeleteMapping(value = "{id}")
    public void deleteDeployment(
            @PathVariable("id") Deployment deployment
    ) {
        deleteDeploymentService.deleteDeployment(deployment);
    }

    @GetMapping(value = "sessions/launch")
    public List<SessionWithCourseDto> getSessionToLaunch(
            @RequestParam(value = "startTime", required = false) Long startTime
    ) {
        LocalDateTime startLocalDateTime = LocalDateTime.now();
        if (startTime != null) {
            startLocalDateTime = Instant.ofEpochMilli(startTime).atZone(zoneId).toLocalDateTime();
        }
        return courseSessionRepository.findByLaunchDeploymentIsNullAndStartDateTimeAfterOrderByStartDateTime(
                startLocalDateTime
        )
                .stream()
                .map(sessionWithCourseMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "sessions/clean")
    public List<SessionWithCourseDto> getSessionToClean() {
        return courseSessionRepository.findByCleanDeploymentIsNullAndLaunchDeploymentIsNotNullOrderByStartDateTime()
                .stream()
                .map(sessionWithCourseMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "worker/{sessionId}")
    public List<OVHRegionWorkerWithSessionDto> getRegionWorkerToCleanFromSession(
            @PathVariable("sessionId") CourseSession courseSession
    ) {
        return ovhRegionWorkerRepository.findOVHRegionWorkerByLaunchDeploymentSessionsToLaunchAndCleanDeploymentIsNull(
                courseSession
        ).stream()
                .map(ovhRegionWorkerWithSessionMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "worker/clean")
    public List<OVHRegionWorkerWithSessionDto> getRegionWorkerToClean(
            @RequestParam(value = "startTime") @NotNull Long startTime
    ) {
        return ovhRegionWorkerRepository
                .findOVHRegionWorkerByCleanDeploymentIsNullAndLaunchDeploymentStartDateTimeBefore(
                    Instant.ofEpochMilli(startTime).atZone(zoneId).toLocalDateTime()
                )
                .stream()
                .map(ovhRegionWorkerWithSessionMapper::convertToDto)
                .collect(Collectors.toList());
    }
}
