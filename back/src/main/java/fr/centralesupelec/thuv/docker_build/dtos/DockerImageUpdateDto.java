package fr.centralesupelec.thuv.docker_build.dtos;

import fr.centralesupelec.thuv.dtos.PortDto;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
public class DockerImageUpdateDto {
    @NotEmpty
    private String description;
    @NotEmpty
    private String dockerFile;
    private String wrapperScript;
    @NotEmpty
    @Valid
    private List<PortDto> ports;
    private MultipartFile contextFolder;
    @NotNull
    private Boolean visible = false;
}
