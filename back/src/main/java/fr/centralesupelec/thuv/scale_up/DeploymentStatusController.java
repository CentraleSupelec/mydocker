package fr.centralesupelec.thuv.scale_up;

import fr.centralesupelec.thuv.scale_up.dtos.DeploymentStatusDto;
import fr.centralesupelec.thuv.scale_up.mappers.DeploymentStatusMapper;
import fr.centralesupelec.thuv.scale_up.model.DeploymentStatus;
import fr.centralesupelec.thuv.scale_up.repository.DeploymentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/deployment_status")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DeploymentStatusController {
    private final DeploymentStatusRepository deploymentStatusRepository;
    private final DeploymentStatusMapper deploymentStatusMapper;

    @GetMapping
    public Page<DeploymentStatusDto> list(
            final Pageable pageable
    ) {
        return deploymentStatusRepository.findByOrderByCreatedOnDesc(pageable)
                .map(deploymentStatusMapper::convertToDto);
    }

    @GetMapping("{id}")
    public DeploymentStatusDto get(
            @PathVariable("id") DeploymentStatus deploymentStatus
    ) {
        return deploymentStatusMapper.convertToDto(deploymentStatus);
    }
}
