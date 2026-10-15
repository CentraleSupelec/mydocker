package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.dtos.ComputeTypeUpdateDto;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ComputeTypeService {
    private static final Logger logger = LoggerFactory.getLogger(ComputeTypeService.class);
    private final ComputeTypeRepository computeTypeRepository;
    private final CourseRepository courseRepository;
    private final OVHRegionWorkerRepository ovhRegionWorkerRepository;
    private final ComputeTypeMapper computeTypeMapper;
    private final InitAutoscalingService initAutoscalingService;

    @Value("${deployment_enabled}")
    private boolean deploymentEnabled;

    public ComputeType updateComputeType(ComputeTypeUpdateDto dto, ComputeType computeType) {
        computeTypeMapper.updateComputeType(dto, computeType);
        this.computeTypeRepository.saveAndFlush(computeType);
        this.validateEmptyTechnicalName(computeType.getCluster());
        if (deploymentEnabled && computeType.getCluster().getDeploymentEnabled()) {
            try {
                initAutoscalingService.sendInitRequest(computeType.getCluster());
                logger.info("Successfully updated autoscaling config");
            } catch (Exception e) {
                logger.error(String.format("Could not update autoscaling config : %s", e.getMessage()), e);
            }
        }
        return computeType;
    }

    public void validateEmptyTechnicalName(Cluster cluster) {
        if (1 != computeTypeRepository.countByTechnicalNameAndCluster("", cluster)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "There must be exactly one ComputeType with empty technical name"
            );
        }
    }

    public void deleteComputeType(ComputeType computeType) {
        long courses = courseRepository.countByComputeType(computeType);
        if (courses > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Compute type is used by %d course(s); move them to another compute type first", courses)
            );
        }
        long workers = ovhRegionWorkerRepository.countByComputeType(computeType);
        if (workers > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Compute type is used by %d deployment worker(s)", workers)
            );
        }
        try {
            this.computeTypeRepository.delete(computeType);
            this.computeTypeRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // A course or worker took this compute type after the counts above.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compute type is still in use", e);
        }
        this.validateEmptyTechnicalName(computeType.getCluster());
    }
}
