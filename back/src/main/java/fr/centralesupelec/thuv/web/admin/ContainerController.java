package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.gRPC.AdminContainerRequest;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.gRPC.RequestPort;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.model.ConnectionType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.service.RequestAdminContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController("adminContainerController")
@RequestMapping("/docker")
public class ContainerController {
    private static final Integer ADMIN_PORT = 80;

    private final ContainerStorage containerStorage;
    private final RequestAdminContainerService requestAdminContainerService;
    private final UserRepository userRepository;

    @Autowired
    public ContainerController(
            ContainerStorage containerStorage,
            RequestAdminContainerService requestAdminContainerService,
            UserRepository userRepository
    ) {
        this.containerStorage = containerStorage;
        this.requestAdminContainerService = requestAdminContainerService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/initAdminContainer/{courseId}")
    @PreAuthorize("@coursePermissionService.canRead(#course)")
    public void initAdminContainer(
            @PathVariable("courseId") Course course,
            @RequestParam(defaultValue = "false", required = false) Boolean forceRecreate,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );

        AdminContainerRequest adminRequest = AdminContainerRequest.newBuilder()
                .setForceRecreate(forceRecreate)
                .setCourseID(String.valueOf(course.getId()))
                .setCourseName(course.getTitle())
                .setUserName(user.getEmail())
                .setPort(
                        RequestPort.newBuilder()
                                .setPortToMap(ADMIN_PORT)
                                .setConnexionType(ConnectionType.HTTP.toString())
                                .setDescription("Explorateur de fichier")
                                .setRequiredToAccessContainer(true)
                                .build()
                )
                .build();
        requestAdminContainerService.requestContainer(adminRequest);
    }

    @RequestMapping(value = "admin/{courseId}", method = RequestMethod.GET)
    @PreAuthorize("@coursePermissionService.canRead(#course)")
    public ContainerDto getAdminContainer(
            @PathVariable("courseId") Course course
    ) {
        Optional<ContainerDto> optionalContainer = containerStorage.getAdminContainer(course.getId());
        return optionalContainer.orElse(null);
    }
}
