package fr.centralesupelec.thuv.docker_build.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
public class DockerImageCreationDto extends DockerImageUpdateDto {
    @NotEmpty
    @Pattern(regexp = "[a-z0-9-_]+", message = "Docker image name should only contain letters, numbers, - or _")
    private String name;
}
