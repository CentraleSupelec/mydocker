package fr.centralesupelec.thuv.scale_up.model;

import lombok.Data;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name = "ovh_region")
@Data
public class OVHRegion {
    @NotEmpty
    @Id
    private String region;

    @Column(nullable = false)
    @NotEmpty
    private String imageId;
}
