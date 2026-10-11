package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.dtos.ComputeTypeUpdateDto;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ComputeTypeService {
    private static final Logger logger = LoggerFactory.getLogger(ComputeTypeService.class);
    private final ComputeTypeRepository computeTypeRepository;
    private final ComputeTypeMapper computeTypeMapper;
    private final InitAutoscalingService initAutoscalingService;

    @Value("${deployment_enabled}")
    private boolean deploymentEnabled;

    public ComputeType updateComputeType(ComputeTypeUpdateDto dto, ComputeType computeType) {
        computeTypeMapper.updateComputeType(dto, computeType);
        this.computeTypeRepository.saveAndFlush(computeType);
        this.validateEmptyTechnicalName();
        if (deploymentEnabled) {
            try {
                initAutoscalingService.sendInitRequest();
                logger.info("Successfully updated autoscaling config");
            } catch (Exception e) {
                logger.error(String.format("Could not update autoscaling config : %s", e.getMessage()), e);
            }
        }
        return computeType;
    }

    public void validateEmptyTechnicalName() {
        if (1 != computeTypeRepository.countByTechnicalName("")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "There must be exactly one ComputeType with empty technical name"
            );
        }
    }

    public void deleteComputeType(ComputeType computeType) {
        this.computeTypeRepository.delete(computeType);
        this.validateEmptyTechnicalName();
    }
}
