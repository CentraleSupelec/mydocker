package fr.centralesupelec.thuv.docker_build;

import fr.centralesupelec.thuv.docker_build.dtos.DockerImageBuildDto;
import fr.centralesupelec.thuv.docker_build.dtos.DockerImageCreationDto;
import fr.centralesupelec.thuv.docker_build.dtos.DockerImageDto;
import fr.centralesupelec.thuv.docker_build.dtos.DockerImageUpdateDto;
import fr.centralesupelec.thuv.docker_build.mappers.DockerImageCreationAndUpdateMapper;
import fr.centralesupelec.thuv.docker_build.mappers.DockerImageMapper;
import fr.centralesupelec.thuv.docker_build.model.BuildStatus;
import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageBuildRepository;
import fr.centralesupelec.thuv.docker_build.repository.DockerImageRepository;
import fr.centralesupelec.thuv.docker_build.service.DockerImageListService;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class DockerImageController {
    private static final Logger logger = LoggerFactory.getLogger(DockerImageController.class);
    private final DockerImageCreationAndUpdateMapper dockerImageCreationAndUpdateMapper;
    private final UserRepository userRepository;
    private final DockerImageRepository dockerImageRepository;
    private final DockerImageMapper dockerImageMapper;
    private final DockerImageBuildRepository dockerImageBuildRepository;
    private final DockerImageBuildService dockerImageBuildService;
    private final ReentrantLock lock = new ReentrantLock();
    private final DockerImageListService dockerImageListService;
    @Value("${context_save_path}")
    private String contextSavePath;

    @PreAuthorize("hasRole('TEACHER')")
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DockerImageDto createDockerImage(
            @ModelAttribute @Valid DockerImageCreationDto dockerImageCreationDto,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        DockerImage dockerImage = dockerImageCreationAndUpdateMapper.convertDtoToModel(dockerImageCreationDto);
        User user = userRepository.getReferenceById(principal.getUserId());
        dockerImage.setCreator(user);
        dockerImageRepository.save(dockerImage);
        return dockerImageMapper.convertToDto(dockerImage);
    }

    @PreAuthorize("@dockerImagePermissionService.canRead(#dockerImage)")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public DockerImageDto getDockerImage(
            @PathVariable("id") DockerImage dockerImage
    ) {
        return dockerImageMapper.convertToDto(dockerImage);
    }

    @PreAuthorize("@dockerImagePermissionService.canRead(#dockerImage)")
    @RequestMapping(value = "/{id}/build", method = RequestMethod.GET)
    public List<DockerImageBuildDto> getDockerImageBuild(
            @PathVariable("id") DockerImage dockerImage
    ) {
        return dockerImageBuildRepository
                .findDockerImageBuildByDockerImageOrderByUpdatedOnDesc(dockerImage)
                .stream()
                .map(dockerImageMapper::convertBuildToDto)
                .collect(Collectors.toList());
    }

    @PreAuthorize("@dockerImagePermissionService.canEdit(#dockerImage)")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DockerImageDto updateDockerImage(
            @PathVariable("id") DockerImage dockerImage,
            @ModelAttribute @Valid DockerImageUpdateDto dto
    ) {
        dockerImageCreationAndUpdateMapper.applyChange(dockerImage, dto);
        dockerImageRepository.save(dockerImage);
        return dockerImageMapper.convertToDto(dockerImage);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Page<DockerImageDto> getDockerImageList(
            @RequestParam(value = "search", required = false) String search,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal,
            Pageable pageable
    ) {
        User user = userRepository.getReferenceById(principal.getUserId());
        return dockerImageListService.getViewableDockerImage(user, search, pageable)
                .map(dockerImageMapper::convertToDto);
    }

    @PreAuthorize("@dockerImagePermissionService.canEdit(#dockerImage)")
    @RequestMapping(value = "/{id}/build", method = RequestMethod.POST)
    public ResponseEntity<?> buildDockerImage(
            @PathVariable("id") DockerImage dockerImage
    ) {
        Optional<DockerImageBuild> dockerImageBuildOptional = dockerImage.getBuilds()
                .stream()
                .filter(dockerImageBuild -> dockerImageBuild.getStatus().equals(BuildStatus.BUILDING))
                .findAny();
        if (dockerImageBuildOptional.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("An image is already being build");
        }

        DockerImageBuild dockerImageBuild = new DockerImageBuild()
                .setDockerImage(dockerImage)
                .setStatus(BuildStatus.BUILDING);
        dockerImageBuildRepository.saveAndFlush(dockerImageBuild);
        try {
            lock.lock();
            dockerImageBuildService.requestDockerImageBuild(dockerImage, dockerImageBuild);
        } catch (IOException e) {
            logger.error("Error while sending context to docker.");
            dockerImageBuild
                    .setStatus(BuildStatus.ERROR)
                    .setBuildErrors("Error while sending context to docker.");
            dockerImageBuildRepository.saveAndFlush(dockerImageBuild);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            lock.unlock();
        }
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@dockerImagePermissionService.canRead(#dockerImage)")
    @GetMapping(
            value = "/context/{id}",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public @ResponseBody FileSystemResource getContext(
            @PathVariable("id") DockerImage dockerImage
    ) {
        File file = new File(contextSavePath + dockerImage.getContextFolderName());
        return new FileSystemResource(file);
    }
}
