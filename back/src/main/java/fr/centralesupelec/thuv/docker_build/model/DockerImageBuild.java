package fr.centralesupelec.thuv.docker_build.model;

import static java.lang.Integer.MAX_VALUE;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "docker_image_build")
public class DockerImageBuild {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private BuildStatus status;

    private String imageName;

    @Column(length = MAX_VALUE)
    private String buildErrors;

    @Column(length = MAX_VALUE)
    private String logs;

    @UpdateTimestamp
    private LocalDateTime updatedOn;

    @ManyToOne
    @JoinColumn(name = "docker_image_id")
    @NotNull
    private DockerImage dockerImage;
}
