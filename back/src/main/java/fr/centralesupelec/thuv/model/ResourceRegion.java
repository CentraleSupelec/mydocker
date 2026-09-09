package fr.centralesupelec.thuv.model;

import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
    name = "resource",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {
            "compute_type_id",
            "resource_id",
            "region_region"
        })
    }
)
@Data
public class ResourceRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ComputeType computeType;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private OVHResource resource;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private OVHRegion region;
}
