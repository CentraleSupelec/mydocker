package fr.centralesupelec.thuv.model;

import lombok.Data;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compute_types")
@Data
public class ComputeType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String displayName;
    @NotNull
    @Column(unique = true)
    private String technicalName;
    private boolean gpu = false;

    @OneToMany(
        mappedBy = "computeType",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @EqualsAndHashCode.Exclude
    private Set<ResourceRegion> autoscalingResourcesRegions =
        new HashSet<>();

    @Column(columnDefinition = "INT8 DEFAULT 0", nullable = false)
    private Long minIdleNodesCount = 0L;
    @Column(columnDefinition = "INT8 DEFAULT 0", nullable = false)
    private Long maxNodesCount = 0L;
    @Column(columnDefinition = "INT8 DEFAULT 0")
    private Long manualNodesCount = 0L;

    @Enumerated(EnumType.STRING)
    @NotNull
    private StorageBackend storageBackend;

    public boolean isAutoscalingConfigured() {
        return (!this.getAutoscalingResourcesRegions().isEmpty())
                && this.getMaxNodesCount() > 0;
    }
}
