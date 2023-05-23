package fr.centralesupelec.thuv.docker_build.model;

import static java.lang.Integer.MAX_VALUE;

import fr.centralesupelec.thuv.model.Port;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.models.DockerImagePermission;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Entity
@Table(name = "docker_image")
public class DockerImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @Column(length = MAX_VALUE)
    @NotBlank
    private String dockerFile;

    @Column(length = MAX_VALUE)
    @NotBlank
    private String wrapperScript;

    private String contextFolderName;

    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "dockerImage")
    @EqualsAndHashCode.Exclude
    private Set<Port> ports = new HashSet<>();

    public DockerImage setPorts(Set<Port> ports) {
        this.ports.clear();
        this.ports.addAll(
                ports.stream()
                    .map(port -> port.setDockerImage(this))
                    .collect(Collectors.toSet())
        );
        return this;
    }

    @OneToMany(mappedBy = "dockerImage", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private Set<DockerImageBuild> builds = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @NotNull
    private User creator;

    @NotNull
    private Boolean visible = false;

    @CreationTimestamp
    private LocalDateTime createdOn;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dockerImage")
    @EqualsAndHashCode.Exclude
    private Set<DockerImagePermission> permissions;
}
