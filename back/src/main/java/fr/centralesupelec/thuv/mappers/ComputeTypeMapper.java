package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.gRPC.InstanceRegionsConfig;
import fr.centralesupelec.gRPC.OwnerAutoscalingConfig;
import fr.centralesupelec.gRPC.ScalingRegion;
import fr.centralesupelec.thuv.dtos.ComputeTypeDto;
import fr.centralesupelec.thuv.dtos.ComputeTypeUpdateDto;
import fr.centralesupelec.thuv.dtos.ResourceRegionsDto;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.ResourceRegion;
import fr.centralesupelec.thuv.model.StorageBackend;
import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import fr.centralesupelec.thuv.scale_up.repository.ResourceRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComputeTypeMapper {
    private final OVHRegionRepository ovhRegionRepository;
    private final OVHResourceRepository ovhResourceRepository;
    private final ResourceRegionRepository resourceRegionRepository;

    public void updateComputeType(ComputeTypeUpdateDto dto, ComputeType computeType) {
        if (
                computeType.getTechnicalName() != null
                        && computeType.getTechnicalName().isEmpty()
                        && !dto.getTechnicalName().isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one compute type with empty technical name is needed"
            );
        }
        computeType.getAutoscalingResourcesRegions().clear();
        if (dto.getAutoscalingResourcesRegions() != null) {
            for (ResourceRegionsDto resourceRegionDto : dto.getAutoscalingResourcesRegions()) {
                Optional<OVHResource> ovhResource = ovhResourceRepository.findById(resourceRegionDto.getOvhResourceId());
                if (!ovhResource.isPresent()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            String.format("OVH Resource %s not found", resourceRegionDto.getOvhResourceId())
                    );
                }

                for (String region : resourceRegionDto.getRegions()) {
                    Optional<OVHRegion> ovhRegion = ovhRegionRepository.findById(region);
                    if (!ovhRegion.isPresent()) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                String.format("OVH Region %s not found", region)
                        );
                    }
                    Optional<ResourceRegion> resourceRegionOptional = resourceRegionRepository.findByComputeTypeAndResourceAndRegion(
                        computeType, ovhResource.get(), ovhRegion.get()
                    );
                    ResourceRegion resourceRegion;
                    if (!resourceRegionOptional.isPresent()) {
                        resourceRegion = new ResourceRegion().setComputeType(computeType).setResource(ovhResource.get()).setRegion(ovhRegion.get());
                    } else {
                        resourceRegion = resourceRegionOptional.get();
                    }
                    computeType.getAutoscalingResourcesRegions().add(resourceRegion);
                }
            }
        }
        if (dto.getMaxNodesCount() != null && dto.getMinIdleNodesCount() != null) {
            if (dto.getMaxNodesCount() < dto.getMinIdleNodesCount()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Max nodes count (%d) is lower than min idle nodes count (%d)",
                                dto.getMaxNodesCount(),
                                dto.getMinIdleNodesCount()
                        )
                );
            }
            computeType
                    .setMinIdleNodesCount(dto.getMinIdleNodesCount())
                    .setMaxNodesCount(dto.getMaxNodesCount())
            ;
        }
        if (dto.getManualNodesCount() != null) {
            computeType.setManualNodesCount(dto.getManualNodesCount());
        }
        computeType
                .setGpu(dto.isGpu())
                .setDisplayName(dto.getDisplayName())
                .setTechnicalName(dto.getTechnicalName())
                .setStorageBackend(StorageBackend.valueOf(dto.getStorageBackend()))
        ;
    }

    public ComputeTypeDto convertToComputeTypeDto(ComputeType computeType) {
        Set<ResourceRegionsDto> autoscalingResourcesRegions =
        computeType.getAutoscalingResourcesRegions()
            .stream()
            .collect(Collectors.groupingBy(
                rr -> rr.getResource().getId()
            ))
            .values()
            .stream()
            .map(resourceRegions -> {
                ResourceRegion first = resourceRegions.iterator().next();

                ResourceRegionsDto dto = new ResourceRegionsDto();
                dto.setOvhResourceId(first.getResource().getId());
                dto.setRegions(
                    resourceRegions.stream()
                        .map(rr -> rr.getRegion().getRegion())
                        .collect(Collectors.toSet())
                );

                return dto;
            })
            .collect(Collectors.toSet());

        ComputeTypeDto computeTypeDto = new ComputeTypeDto()
                .setId(computeType.getId())
                .setDisplayName(computeType.getDisplayName())
                .setTechnicalName(computeType.getTechnicalName())
                .setGpu(computeType.isGpu())
                .setMinIdleNodesCount(computeType.getMinIdleNodesCount())
                .setMaxNodesCount(computeType.getMaxNodesCount())
                .setManualNodesCount(computeType.getManualNodesCount())
                .setStorageBackend(computeType.getStorageBackend().toString())
                .setAutoscalingResourcesRegions(autoscalingResourcesRegions)
            ;


            return computeTypeDto;
    }

    public OwnerAutoscalingConfig convertComputeTypeToOwner(ComputeType computeType) {
        Map<String, List<OVHRegion>> regionsByInstanceType =
        computeType.getAutoscalingResourcesRegions()
            .stream()
            .collect(Collectors.groupingBy(
                resourceRegion -> resourceRegion.getResource().getType(),
                Collectors.mapping(
                    resourceRegion -> resourceRegion.getRegion(),
                    Collectors.toList()
                )
            ));
        return OwnerAutoscalingConfig
                .newBuilder()
                .addAllInstancesRegions(
                    regionsByInstanceType.entrySet()
                        .stream()
                        .map(entry ->
                            InstanceRegionsConfig.newBuilder()
                                .setInstanceType(entry.getKey())
                                .addAllRegions(
                                    entry.getValue()
                                        .stream()
                                        .map(ovhRegion -> ScalingRegion
                                            .newBuilder()
                                            .setRegion(ovhRegion.getRegion())
                                            .setImageId(ovhRegion.getImageId())
                                            .build()
                                        )
                                        .collect(Collectors.toList())
                                )

                                .build()
                        )
                        .collect(Collectors.toList())
                )
                .setMaxNodesCount(computeType.getMaxNodesCount())
                .setMinIdleNodesCount(computeType.getMinIdleNodesCount())
                .setManualNodesCount(computeType.getManualNodesCount())
                .build()
                ;
    }
}
