package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.thuv.dtos.SlurmClusterAdminDto;
import fr.centralesupelec.thuv.dtos.SlurmClusterDto;
import fr.centralesupelec.thuv.dtos.SlurmClusterRequestDto;
import fr.centralesupelec.thuv.mappers.SlurmClusterMapper;
import fr.centralesupelec.thuv.model.SlurmCluster;
import fr.centralesupelec.thuv.repository.SlurmClusterRepository;
import fr.centralesupelec.thuv.service.SlurmClusterService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController("adminSlurmClusterController")
@RequestMapping("admin/slurm-clusters")
@RequiredArgsConstructor
public class SlurmClusterController {

    private final SlurmClusterRepository slurmClusterRepository;
    private final SlurmClusterService slurmClusterService;
    private final SlurmClusterMapper slurmClusterMapper;

    // Course form dropdown: id + displayName. Both lists read a projection without the
    // secret, so one undecryptable cluster secret cannot break the course pages.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/")
    public List<SlurmClusterDto> getSlurmClusters() {
        return slurmClusterRepository.findAllByOrderById().stream()
                .map(slurmClusterMapper::convertToDto)
                .collect(Collectors.toList());
    }

    // Admin management list: id + displayName + technicalName + apiEndpoint, never the secret.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/details")
    public List<SlurmClusterAdminDto> getSlurmClusterDetails() {
        return slurmClusterRepository.findAllByOrderById().stream()
                .map(slurmClusterMapper::convertToAdminDto)
                .collect(Collectors.toList());
    }

    // Returns the admin projection (no secret): the edit form prefills from
    // this, and the secret is write-only so it must never be sent back.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{id}")
    public SlurmClusterAdminDto getSlurmCluster(
            @PathVariable("id") SlurmCluster slurmCluster
    ) {
        return slurmClusterMapper.convertToAdminDto(slurmCluster);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "")
    public SlurmClusterAdminDto newSlurmCluster(
            @RequestBody @Valid SlurmClusterRequestDto dto
    ) {
        // Secret is optional on the DTO (blank keeps it on update); on create
        // it must be present.
        if (StringUtils.isBlank(dto.getSecretKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "secretKey is required");
        }
        return slurmClusterMapper.convertToAdminDto(slurmClusterService.createSlurmCluster(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}")
    public SlurmClusterAdminDto editSlurmCluster(
            @PathVariable("id") SlurmCluster slurmCluster,
            @RequestBody @Valid SlurmClusterRequestDto dto
    ) {
        return slurmClusterMapper.convertToAdminDto(
                slurmClusterService.updateSlurmCluster(slurmCluster, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public void deleteSlurmCluster(
            @PathVariable("id") SlurmCluster slurmCluster
    ) {
        slurmClusterService.deleteSlurmCluster(slurmCluster);
    }
}
