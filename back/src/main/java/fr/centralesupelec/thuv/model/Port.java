package fr.centralesupelec.thuv.model;

import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "ports")
@Data
public class Port {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String description;

    @NotNull
    private Integer mapPort;

    @NotNull
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requiredToAccessContainer;

    @Enumerated(EnumType.STRING)
    private ConnectionType connectionType;

    @ManyToOne
    @JoinColumn
    private Course course;

    @ManyToOne
    @JoinColumn
    private DockerImage dockerImage;
}
