package fr.centralesupelec.thuv.permissions.repository;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.DockerImagePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

public interface DockerImagePermissionRepository extends JpaRepository<DockerImagePermission, Long> {
    Optional<DockerImagePermission> findByUserAndDockerImage(@NotNull User user, @NotNull DockerImage dockerImage);
    List<DockerImagePermission> findByUser(@NotNull User user);
    List<DockerImagePermission> findByDockerImage(@NotNull DockerImage dockerImage);
}
