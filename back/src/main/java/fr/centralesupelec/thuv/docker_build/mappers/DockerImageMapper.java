package fr.centralesupelec.thuv.docker_build.mappers;

import fr.centralesupelec.thuv.docker_build.dtos.DockerImageBuildDto;
import fr.centralesupelec.thuv.docker_build.dtos.DockerImageDto;
import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.mappers.PortsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DockerImageMapper {
    private final PortsMapper portsMapper;
    private final ZoneId zoneId;

    private static class DockerImageBuildComparator implements Comparator<DockerImageBuild> {
        @Override
        public int compare(DockerImageBuild o1, DockerImageBuild o2) {
            return o2.getUpdatedOn().compareTo(o1.getUpdatedOn());
        }
    }

    public DockerImageDto convertToDto(DockerImage dockerImage) {
        return new DockerImageDto()
                .setId(
                        dockerImage.getId()
                )
                .setName(
                        dockerImage.getName()
                )
                .setVisible(
                        dockerImage.getVisible()
                )
                .setCreator(
                        dockerImage.getCreator().getName() + " " + dockerImage.getCreator().getLastname()
                )
                .setDescription(
                        dockerImage.getDescription()
                )
                .setDockerFile(
                        dockerImage.getDockerFile()
                )
                .setWrapperScript(
                        dockerImage.getWrapperScript()
                )
                .setContextFolderName(
                        dockerImage.getContextFolderName()
                )
                .setLastStatus(
                        dockerImage.getBuilds()
                                .stream()
                                .sorted(new DockerImageBuildComparator())
                                .map(DockerImageBuild::getStatus)
                                .findFirst()
                                .orElse(null)
                )
                .setCreatedOn(
                        dockerImage.getCreatedOn().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setPorts(
                        dockerImage
                                .getPorts()
                                .stream()
                                .map(portsMapper::convertToDTO)
                                .collect(Collectors.toSet())
                );
    }

    public DockerImageBuildDto convertBuildToDto(DockerImageBuild dockerImageBuild) {
        return new DockerImageBuildDto()
                .setId(
                        dockerImageBuild.getId()
                )
                .setImageName(
                        dockerImageBuild.getImageName()
                )
                .setStatus(
                        dockerImageBuild.getStatus()
                )
                .setBuildErrors(
                        dockerImageBuild.getBuildErrors()
                )
                .setUpdatedOn(
                        dockerImageBuild.getUpdatedOn().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setLogs(
                        dockerImageBuild.getLogs()
                );
    }
}
