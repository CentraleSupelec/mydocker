package fr.centralesupelec.thuv.permissions.models;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@DiscriminatorValue("DOCKER_IMAGE")
@EqualsAndHashCode(callSuper = true)
@Data
public class DockerImagePermission extends Permission {
    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    @JoinColumn(name = "docker_image_id", nullable = true)
    private DockerImage dockerImage;
}
