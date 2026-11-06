package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.dtos.SlurmClusterRequestDto;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.SlurmCluster;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.SlurmClusterRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlurmClusterService {

    private final SlurmClusterRepository slurmClusterRepository;
    private final CourseRepository courseRepository;

    public SlurmCluster createSlurmCluster(SlurmClusterRequestDto dto) {
        SlurmCluster cluster = new SlurmCluster();
        cluster.setDisplayName(dto.getDisplayName());
        cluster.setTechnicalName(dto.getTechnicalName());
        cluster.setApiEndpoint(dto.getApiEndpoint());
        cluster.setSecretKey(dto.getSecretKey());
        return slurmClusterRepository.save(cluster);
    }

    public SlurmCluster updateSlurmCluster(SlurmCluster existingCluster, SlurmClusterRequestDto dto) {
        existingCluster.setDisplayName(dto.getDisplayName());
        existingCluster.setTechnicalName(dto.getTechnicalName());
        existingCluster.setApiEndpoint(dto.getApiEndpoint());
        // Write-only secret: only overwrite when a new value is actually sent,
        // so an edit that leaves the field blank keeps the stored secret and
        // the secret never has to be round-tripped to the client.
        if (StringUtils.isNotBlank(dto.getSecretKey())) {
            existingCluster.setSecretKey(dto.getSecretKey());
        }
        return slurmClusterRepository.save(existingCluster);
    }

    // The courses.slurm_cluster_id foreign key declares no ON DELETE behaviour,
    // so deleting a cluster a course still points at surfaces as a raw database
    // constraint violation with no indication of what is blocking it. Refuse the
    // delete and name the courses instead, so the admin can unbind them first
    public void deleteSlurmCluster(SlurmCluster slurmCluster) {
        List<Course> boundCourses = courseRepository.findBySlurmCluster(slurmCluster);
        if (!boundCourses.isEmpty()) {
            String titles = boundCourses.stream()
                    .map(Course::getTitle)
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cluster is still assigned to " + boundCourses.size()
                            + " course(s): " + titles
                            + ". Unassign it from them before deleting."
            );
        }
        slurmClusterRepository.delete(slurmCluster);
    }
}
