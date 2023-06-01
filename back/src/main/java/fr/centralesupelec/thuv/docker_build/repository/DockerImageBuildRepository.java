package fr.centralesupelec.thuv.docker_build.repository;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DockerImageBuildRepository extends JpaRepository<DockerImageBuild, Long> {
    List<DockerImageBuild> findDockerImageBuildByDockerImageOrderByUpdatedOnDesc(DockerImage dockerImage);
}
