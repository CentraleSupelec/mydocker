package fr.centralesupelec.thuv.scale_up;

import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/ovh-region")
@PreAuthorize("hasRole('ADMIN')")
public class OVHRegionController {
    private final OVHRegionRepository ovhRegionRepository;

    @Autowired
    public OVHRegionController(OVHRegionRepository ovhRegionRepository) {
        this.ovhRegionRepository = ovhRegionRepository;
    }

    @GetMapping
    public List<OVHRegion> getRegions() {
        return ovhRegionRepository.findAll();
    }

    @GetMapping("min")
    public List<String> getStringRegions() {
        return ovhRegionRepository.findAll().stream()
                .map(OVHRegion::getRegion)
                .collect(Collectors.toList());
    }

    @PostMapping
    public void createOVhRegion(
            @RequestBody @Valid OVHRegion region
    ) {
        ovhRegionRepository.save(region);
    }

    @PutMapping("{id}")
    public void updateOVHRegion(
            @PathVariable("id") OVHRegion region,
            @RequestBody @NotEmpty String imageId
        ) {
        region.setImageId(imageId);
        ovhRegionRepository.save(region);
    }

    @DeleteMapping("{id}")
    public void deleteOVHRegion(
            @PathVariable("id") OVHRegion region
    ) {
        ovhRegionRepository.delete(region);
    }
}
