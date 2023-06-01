package fr.centralesupelec.thuv.docker_build.repository;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface DockerImageRepository extends JpaRepository<DockerImage, Long>, JpaSpecificationExecutor<DockerImage> {
    List<DockerImage> findByCreator(@NotNull User creator);

    Page<DockerImage> findAllBy(Pageable pageable);
    Page<DockerImage> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
}
