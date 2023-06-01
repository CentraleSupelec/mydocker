package fr.centralesupelec.thuv.docker_build.mappers;

import fr.centralesupelec.thuv.docker_build.dtos.DockerImageCreationDto;
import fr.centralesupelec.thuv.docker_build.dtos.DockerImageUpdateDto;
import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.mappers.PortsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DockerImageCreationAndUpdateMapper {
    private static final Logger logger = LoggerFactory.getLogger(DockerImageCreationAndUpdateMapper.class);
    @Value("${context_save_path}")
    private String contextSavePath;
    private final PortsMapper portsMapper;

    @Autowired
    public DockerImageCreationAndUpdateMapper(PortsMapper portsMapper) {
        this.portsMapper = portsMapper;
    }


    public DockerImage convertDtoToModel(DockerImageCreationDto dto) {
        DockerImage dockerImage = new DockerImage()
                .setName(
                        dto.getName()
                )
                .setVisible(
                        dto.getVisible()
                )
                .setDescription(
                        dto.getDescription()
                )
                .setDockerFile(
                        dto.getDockerFile()
                )
                .setWrapperScript(
                        dto.getWrapperScript()
                )
                .setPorts(
                        dto
                                .getPorts()
                                .stream()
                                .map(portsMapper::convertDTOToModel)
                                .collect(Collectors.toSet())
                );
        if (dto.getContextFolder() != null) {
            dockerImage.setContextFolderName(
                    convertContextFolder(
                            dto.getContextFolder()
                    )
            );
        }
        return dockerImage;
    }

    public void applyChange(DockerImage dockerImage, DockerImageUpdateDto dto) {
        dockerImage.setDescription(
                dto.getDescription()
        )
        .setVisible(
                dto.getVisible()
        )
        .setDockerFile(
                dto.getDockerFile()
        )
        .setWrapperScript(
                dto.getWrapperScript()
        )
        .setPorts(
                dto
                        .getPorts()
                        .stream()
                        .map(portsMapper::convertDTOToModel)
                        .collect(Collectors.toSet())
        );

        if (dto.getContextFolder() != null && !dto.getContextFolder().isEmpty()) {
            dockerImage.setContextFolderName(
                    convertContextFolder(
                            dto.getContextFolder()
                    )
            );
        }
        if (dto.getContextFolder() == null) {
            dockerImage.setContextFolderName(null);
        }
    }

    private String convertContextFolder(MultipartFile contextFolder) {
        try {
            File destFile = new File(contextSavePath + UUID.randomUUID() + "-" + contextFolder.getOriginalFilename());
            contextFolder.transferTo(destFile);
            return destFile.getName();
        } catch (IOException e) {
            logger.error("Cannot copy context folder, error: " + e);
            return null;
        }
    }
}
