package fr.centralesupelec.thuv.docker_build.dtos;

import fr.centralesupelec.thuv.docker_build.model.BuildStatus;
import fr.centralesupelec.thuv.dtos.PortDto;
import lombok.Data;

import java.util.Set;

@Data
public class DockerImageDto {
    private Long id;
    private String name;
    private String description;
    private String dockerFile;
    private String wrapperScript;
    private String contextFolderName;
    private Set<PortDto> ports;
    private BuildStatus lastStatus;
    private Boolean visible;
    private String creator;
    private Long createdOn;
}
