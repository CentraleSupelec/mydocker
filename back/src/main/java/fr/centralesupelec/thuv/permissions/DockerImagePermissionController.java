package fr.centralesupelec.thuv.permissions;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.permissions.dtos.PermissionDto;
import fr.centralesupelec.thuv.permissions.dtos.UpdatePermissionDto;
import fr.centralesupelec.thuv.permissions.mappers.UserMapper;
import fr.centralesupelec.thuv.permissions.models.DockerImagePermission;
import fr.centralesupelec.thuv.permissions.repository.DockerImagePermissionRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("permissions/docker-image")
@RequiredArgsConstructor
public class DockerImagePermissionController {
    private final UserRepository userRepository;
    private final DockerImagePermissionRepository dockerImagePermissionRepository;
    private final UserMapper userMapper;

    @PutMapping("{id}")
    @PreAuthorize("@dockerImagePermissionService.canEditPermission(#dockerImage)")
    public void giveDockerImagePermission(
            @PathVariable("id") DockerImage dockerImage,
            @RequestBody @Valid UpdatePermissionDto updatePermissionDto
    ) {
        DockerImagePermission dockerImagePermission = (DockerImagePermission) new DockerImagePermission()
                .setDockerImage(
                        dockerImage
                )
                .setId(
                        updatePermissionDto.getId()
                )
                .setType(
                        updatePermissionDto.getType()
                )
                .setUser(
                        userRepository.getReferenceById(
                                updatePermissionDto.getUserId()
                        )
                );
        dockerImagePermissionRepository.save(dockerImagePermission);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("@dockerImagePermissionService.canEditPermission(#dockerImagePermission.dockerImage)")
    public void deleteDockerImagePermission(
            @PathVariable("id") DockerImagePermission dockerImagePermission
    ) {
        dockerImagePermissionRepository.delete(dockerImagePermission);
    }

    @GetMapping("{id}")
    @PreAuthorize("@dockerImagePermissionService.canEditPermission(#dockerImage)")
    public List<PermissionDto> listDockerImagePermission(
            @PathVariable("id") DockerImage dockerImage
    ) {
        return dockerImagePermissionRepository.findByDockerImage(
                dockerImage
        )
                .stream()
                .map(
                        p -> new PermissionDto()
                                .setType(p.getType())
                                .setUser(
                                        userMapper.convertToDto(p.getUser())
                                )
                                .setId(
                                        p.getId()
                                )
                )
                .collect(Collectors.toList());
    }
}
