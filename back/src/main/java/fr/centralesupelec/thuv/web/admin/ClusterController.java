package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.thuv.dtos.ClusterBasicView;
import fr.centralesupelec.thuv.dtos.ClusterUpdateDto;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.service.ClusterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController("adminClusterController")
@RequestMapping("admin/clusters")
@RequiredArgsConstructor
public class ClusterController {
    private final ClusterRepository clusterRepository;
    private final ClusterService clusterService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/")
    public List<Cluster> getClusters() {
        return clusterRepository.findAll(Sort.by("id"));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping(value = "/partial_information")
    public List<ClusterBasicView> getClustersPartialInformation() {
        return clusterRepository.findAllByOrderById();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{id}")
    public Cluster getCluster(
            @PathVariable("id") Cluster cluster
    ) {
        return cluster;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "")
    public Cluster newCluster(
            @RequestBody @Valid ClusterUpdateDto dto
    ) {
        Cluster cluster = new Cluster();
        return clusterService.updateCluster(dto, cluster);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}")
    public Cluster editCluster(
            @PathVariable("id") Cluster cluster,
            @RequestBody @Valid ClusterUpdateDto dto
    ) {
        return clusterService.updateCluster(dto, cluster);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public void deleteCluster(
            @PathVariable("id") Cluster cluster
    ) {
        clusterService.deleteCluster(cluster);
    }
}
