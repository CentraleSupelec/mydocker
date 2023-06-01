package fr.centralesupelec.thuv.scale_up;

import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.repository.CourseSessionRepository;
import fr.centralesupelec.thuv.scale_up.deployment_scheduler.DeploymentScheduler;
import fr.centralesupelec.thuv.scale_up.dtos.CourseSessionWithResourceDto;
import fr.centralesupelec.thuv.scale_up.dtos.OVHResourceDto;
import fr.centralesupelec.thuv.scale_up.dtos.terraform_state.TerraformInstance;
import fr.centralesupelec.thuv.scale_up.dtos.terraform_state.TerraformInstanceRessource;
import fr.centralesupelec.thuv.scale_up.dtos.terraform_state.TerraformStateDto;
import fr.centralesupelec.thuv.scale_up.exception.InvalidTerraformState;
import fr.centralesupelec.thuv.scale_up.exception.TerraformStateNotFound;
import fr.centralesupelec.thuv.scale_up.mappers.CourseSessionWithResourceMapper;
import fr.centralesupelec.thuv.scale_up.mappers.OVHResourceMapper;
import fr.centralesupelec.thuv.scale_up.model.DeploymentStatus;
import fr.centralesupelec.thuv.scale_up.repository.DeploymentStatusRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/scale")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AutoScaleUpController {
    private final OVHResourceRepository ovhResourceRepository;
    private final OVHResourceMapper ovhResourceMapper;
    private final CourseSessionRepository courseSessionRepository;
    private final CourseSessionWithResourceMapper courseSessionWithResourceMapper;
    private final ScaleUpParameterConfiguration scaleUpParameterConfiguration;
    private final RestTemplate restTemplate;
    private final ZoneId zoneId;
    private final DeploymentScheduler deploymentScheduler;
    private final TaskScheduler taskScheduler;
    private final DeploymentStatusRepository deploymentStatusRepository;

    @GetMapping(value = "ovh-resources")
    public List<OVHResourceDto> getOVHResources() {
        return ovhResourceRepository.findAll().stream()
                .map(ovhResourceMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "session")
    public List<CourseSessionWithResourceDto> getSessions(
            @RequestParam(value = "startTime", required = false) Long startTime
    ) {
        LocalDateTime startLocalDateTime = LocalDateTime.now();
        if (startTime != null) {
            startLocalDateTime = Instant.ofEpochMilli(startTime).atZone(zoneId).toLocalDateTime();
        }
        return courseSessionRepository.findByStartDateTimeAfterOrderByStartDateTime(startLocalDateTime)
                .stream()
                .map(
                        courseSessionWithResourceMapper::convertToDto
                )
                .collect(Collectors.toList());
    }

    @GetMapping(value = "session/{id}")
    public CourseSessionWithResourceDto getSessionRessource(
            @PathVariable("id") CourseSession courseSession
    ) {
        return courseSessionWithResourceMapper.convertToDto(
                courseSession
        );
    }

    @PutMapping(value = "session/{id}")
    public void updateSessionRessource(
            @PathVariable("id") CourseSession courseSession,
            @RequestBody @Valid CourseSessionWithResourceDto courseSessionWithResourceDto
    ) {
        courseSessionWithResourceMapper.applyChangesForRessources(
                courseSession,
                courseSessionWithResourceDto
        );
        this.courseSessionRepository.save(courseSession);
    }

    @GetMapping(value = "terraform-state")
    public List<TerraformInstance> getTerraformState() throws TerraformStateNotFound, InvalidTerraformState {
        TerraformStateDto terraformStateDto = restTemplate.getForObject(
                scaleUpParameterConfiguration.getTerraformStateUrl(),
                TerraformStateDto.class
        );
        if (terraformStateDto == null) {
            throw new TerraformStateNotFound();
        }
        return terraformStateDto.getResources()
                .stream()
                .filter(r -> r instanceof TerraformInstanceRessource)
                .flatMap(resource -> ((TerraformInstanceRessource) resource).getInstances().stream())
                .collect(Collectors.toList())

        ;
    }

    @PostMapping
    public void launchDeployment() {
        Optional<DeploymentStatus> optionalDeploymentStatus = deploymentStatusRepository
                .findByStatusInOrderByUpdatedOnDesc(
                        Arrays.asList(DeploymentStatus.Status.RUNNING, DeploymentStatus.Status.CREATED)
                );
        if (optionalDeploymentStatus.isPresent()
                && (
                        optionalDeploymentStatus.get().getStatus() == DeploymentStatus.Status.RUNNING
                        || optionalDeploymentStatus.get().getStatus() == DeploymentStatus.Status.CREATED
                    )
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        this.taskScheduler.schedule(
                deploymentScheduler, Instant.now()
        );
    }
}
