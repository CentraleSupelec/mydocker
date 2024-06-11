package fr.centralesupelec.thuv.model;

import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
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

    @ManyToMany(fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    private Set<OVHRegion> autoscalingRegions = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    private OVHResource autoscalingResource;

    @Column(columnDefinition = "INT8 DEFAULT 0", nullable = false)
    private Long minIdleNodesCount = 0L;
    @Column(columnDefinition = "INT8 DEFAULT 0", nullable = false)
    private Long maxNodesCount = 0L;
    @Column(columnDefinition = "INT8 DEFAULT 0")
    private Long manualNodesCount = 0L;

    public boolean isAutoscalingConfigured() {
        return (!this.getAutoscalingRegions().isEmpty())
                && this.getAutoscalingResource() != null
                && this.getMaxNodesCount() > 0;
    }
}
