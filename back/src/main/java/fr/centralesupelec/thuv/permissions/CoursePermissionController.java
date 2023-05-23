package fr.centralesupelec.thuv.permissions;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.permissions.dtos.PermissionDto;
import fr.centralesupelec.thuv.permissions.dtos.UpdatePermissionDto;
import fr.centralesupelec.thuv.permissions.mappers.UserMapper;
import fr.centralesupelec.thuv.permissions.models.CoursePermission;
import fr.centralesupelec.thuv.permissions.repository.CoursePermissionRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("permissions/course")
@RequiredArgsConstructor
public class CoursePermissionController {
    private final UserRepository userRepository;
    private final CoursePermissionRepository coursePermissionRepository;
    private final UserMapper userMapper;

    @PutMapping("{id}")
    @PreAuthorize("@coursePermissionService.canEditPermission(#course)")
    public void giveDockerImagePermission(
            @PathVariable("id") Course course,
            @RequestBody @Valid UpdatePermissionDto updatePermissionDto
    ) {
        CoursePermission coursePermission = (CoursePermission) new CoursePermission()
                .setCourse(
                        course
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
        coursePermissionRepository.save(coursePermission);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("@coursePermissionService.canEditPermission(#coursePermission.course)")
    public void deleteDockerImagePermission(
            @PathVariable("id") CoursePermission coursePermission
    ) {
        coursePermissionRepository.delete(coursePermission);
    }

    @GetMapping("{id}")
    @PreAuthorize("@coursePermissionService.canEditPermission(#course)")
    public List<PermissionDto> listDockerImagePermission(
            @PathVariable("id") Course course
    ) {
        return coursePermissionRepository.findByCourse(
                course
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
