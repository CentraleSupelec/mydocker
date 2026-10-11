package fr.centralesupelec.thuv.docker_build;

import fr.centralesupelec.thuv.docker_build.dtos.DockerImageDto;
import fr.centralesupelec.thuv.docker_build.mappers.DockerImageMapper;
import fr.centralesupelec.thuv.docker_build.model.BuildStatus;
import fr.centralesupelec.thuv.docker_build.model.DockerImage;
import fr.centralesupelec.thuv.docker_build.model.DockerImageBuild;
import fr.centralesupelec.thuv.mappers.PortsMapper;
import fr.centralesupelec.thuv.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

/**
 * The picker shows the date of each image's last build; every build counts, whatever its status.
 */
class DockerImageMapperLastBuildOnTest {
    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private final DockerImageMapper mapper = new DockerImageMapper(mock(PortsMapper.class), ZONE);

    @Test
    void lastBuildOnIsTheNewestBuild() {
        DockerImage image = image(
                build(LocalDateTime.of(2026, 1, 2, 12, 0), BuildStatus.OK),
                build(LocalDateTime.of(2026, 3, 7, 12, 0), BuildStatus.OK)
        );

        DockerImageDto dto = mapper.convertToDto(image);

        assertEquals(epochMilli(LocalDateTime.of(2026, 3, 7, 12, 0)), dto.getLastBuildOn());
    }

    @Test
    void lastBuildOnCountsEveryStatus() {
        DockerImage image = image(
                build(LocalDateTime.of(2026, 1, 2, 12, 0), BuildStatus.OK),
                build(LocalDateTime.of(2026, 3, 7, 12, 0), BuildStatus.ERROR)
        );

        DockerImageDto dto = mapper.convertToDto(image);

        assertEquals(epochMilli(LocalDateTime.of(2026, 3, 7, 12, 0)), dto.getLastBuildOn());
    }

    @Test
    void lastBuildOnIsNullWithoutBuild() {
        DockerImageDto dto = mapper.convertToDto(image());

        assertNull(dto.getLastBuildOn());
    }

    private static long epochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    private static DockerImageBuild build(LocalDateTime updatedOn, BuildStatus status) {
        DockerImageBuild build = new DockerImageBuild();
        build.setUpdatedOn(updatedOn);
        build.setStatus(status);
        return build;
    }

    private static DockerImage image(DockerImageBuild... builds) {
        User creator = new User();
        creator.setName("Ada");
        creator.setLastname("Lovelace");
        DockerImage image = new DockerImage();
        image.setName("my-image");
        image.setCreator(creator);
        image.setCreatedOn(LocalDateTime.of(2025, 9, 1, 8, 0));
        image.setBuilds(new LinkedHashSet<>(List.of(builds)));
        return image;
    }
}
