package fr.centralesupelec.thuv.scale_up.model;

import fr.centralesupelec.thuv.model.CourseSession;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("LAUNCH")
@EqualsAndHashCode(callSuper = true)
@Data
public class LaunchDeployment extends Deployment {
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "launchDeployment", orphanRemoval = true)
    @NotEmpty
    private Set<OVHRegionWorker> workersToLaunch = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "launchDeployment")
    private Set<CourseSession> sessionsToLaunch = new HashSet<>();
}
