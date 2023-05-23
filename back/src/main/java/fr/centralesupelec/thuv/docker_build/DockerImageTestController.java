package fr.centralesupelec.thuv.docker_build;

import fr.centralesupelec.gRPC.ContainerRequest;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.mappers.PortToGrpcRequestPortMapper;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.service.LogRequestService;
import fr.centralesupelec.thuv.service.RequestContainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/images/build")
@RequiredArgsConstructor
public class DockerImageTestController {
    private final ContainerStorage containerStorage;
    private final RequestContainerService requestContainerService;
    private final PortToGrpcRequestPortMapper portToGrpcRequestPortMapper;
    private final LogRequestService logRequestService;

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ContainerDto getContainer(
            @PathVariable("id") DockerImageBuild dockerImageBuild,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Long userId = principal.getUserId();
        Optional<ContainerDto> optionalContainer = containerStorage.getContainer(
                String.valueOf(userId),
                generateCourseId(dockerImageBuild.getId(), dockerImageBuild.getDockerImage().getId())
        );
        return optionalContainer.orElse(null);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.POST)
    public void initGetContainer(
            @PathVariable("id") DockerImageBuild dockerImageBuild,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        ContainerRequest containerRequest = ContainerRequest.newBuilder()
                .setUserID(String.valueOf(principal.getUserId()))
                .setImageID(dockerImageBuild.getImageName())
                .setCourseID(generateCourseId(dockerImageBuild.getId(), dockerImageBuild.getDockerImage().getId()))
                .addAllPorts(
                        dockerImageBuild
                                .getDockerImage()
                                .getPorts()
                                .stream()
                                .map(portToGrpcRequestPortMapper::mapToRequestPort)
                                .collect(Collectors.toList())
                )
                .build();
        requestContainerService.requestContainer(containerRequest);
    }

    @GetMapping(value = "logs/{id}")
    public String getLogs(
            @PathVariable("id") DockerImageBuild dockerImageBuild,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        Long userId = principal.getUserId();
        return logRequestService.getLog(
                String.valueOf(userId),
                generateCourseId(dockerImageBuild.getId(), dockerImageBuild.getDockerImage().getId())
        );
    }

    private String generateCourseId(Long dockerImageBuildId, Long dockerImageId) {
        return String.format("test-build-%s-for-image-%s", dockerImageBuildId, dockerImageId);
    }
}
