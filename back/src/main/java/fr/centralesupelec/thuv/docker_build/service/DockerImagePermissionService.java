package fr.centralesupelec.thuv.docker_build.service;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.DockerImagePermission;
import fr.centralesupelec.thuv.permissions.models.Permission;
import fr.centralesupelec.thuv.permissions.repository.DockerImagePermissionRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service("dockerImagePermissionService")
@RequiredArgsConstructor
public class DockerImagePermissionService {
    private final UserRepository userRepository;
    private final DockerImagePermissionRepository dockerImagePermissionRepository;

    public boolean canEdit(DockerImage dockerImage) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || dockerImage.getCreator().getId().equals(user.getId())
                || hasPermissionForImage(
                        Collections.singletonList(Permission.Type.edit), dockerImage, user
                );
    }

    public boolean canRead(DockerImage dockerImage) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || dockerImage.getCreator().getId().equals(user.getId())
                || dockerImage.getVisible()
                || hasPermissionForImage(
                        Arrays.asList(Permission.Type.view, Permission.Type.edit), dockerImage, user
                );
    }

    public boolean canEditPermission(DockerImage dockerImage) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || dockerImage.getCreator().getId().equals(user.getId());
    }

    private boolean hasPermissionForImage(List<Permission.Type> types, DockerImage dockerImage, User user) {
        Optional<DockerImagePermission> dockerImagePermissionOptional = dockerImagePermissionRepository
                .findByUserAndDockerImage(user, dockerImage);
        return dockerImagePermissionOptional.isPresent()
                && types.contains(dockerImagePermissionOptional.get().getType());
    }
}
