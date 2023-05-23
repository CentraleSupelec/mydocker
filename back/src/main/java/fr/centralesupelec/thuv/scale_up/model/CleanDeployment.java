package fr.centralesupelec.thuv.scale_up.model;

import fr.centralesupelec.thuv.model.CourseSession;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("CLEAN")
@EqualsAndHashCode(callSuper = true)
@Data
public class CleanDeployment extends Deployment {
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cleanDeployment")
    @NotEmpty
    private Set<OVHRegionWorker> workersToClean = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cleanDeployment", fetch = FetchType.EAGER)
    private Set<CourseSession> sessionsToClean = new HashSet<>();
}
