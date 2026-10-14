package fr.centralesupelec.thuv.docker_build.dtos;

import fr.centralesupelec.thuv.docker_build.model.BuildStatus;
import lombok.Data;

@Data
public class DockerImageBuildDto {
    private Long id;
    private BuildStatus status;
    private String buildErrors;
    private String logs;
    private Long updatedOn;
    private String imageName;
}
