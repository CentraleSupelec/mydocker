package fr.centralesupelec.thuv.scale_up.model;

import fr.centralesupelec.thuv.model.ComputeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "ovh_region_worker")
@Data
public class OVHRegionWorker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private OVHRegion region;

    @ManyToOne
    @JoinColumn(nullable = false)
    private OVHResource ressource;

    @NotNull
    private Long count;

    @ManyToOne
    @JoinColumn()
    @EqualsAndHashCode.Exclude
    private LaunchDeployment launchDeployment;

    @ManyToOne
    @JoinColumn()
    @EqualsAndHashCode.Exclude
    private CleanDeployment cleanDeployment;

    @ManyToOne
    @JoinColumn()
    private ComputeType computeType;
}
