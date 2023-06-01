package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.SessionDto;
import fr.centralesupelec.thuv.dtos.SessionUpdateDto;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.repository.CourseSessionRepository;
import fr.centralesupelec.thuv.scale_up.dtos.CleanDeploymentSummaryDto;
import fr.centralesupelec.thuv.scale_up.dtos.DeploymentSummaryDto;
import fr.centralesupelec.thuv.scale_up.dtos.LaunchDeploymentSummaryDto;
import fr.centralesupelec.thuv.scale_up.model.CleanDeployment;
import fr.centralesupelec.thuv.scale_up.model.Deployment;
import fr.centralesupelec.thuv.scale_up.model.LaunchDeployment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SessionMapper {
    private final ZoneId zoneId;
    private final CourseSessionRepository courseSessionRepository;


    public SessionUpdateDto convertToUpdateDto(CourseSession courseSession) {
        return new SessionUpdateDto()
                .setId(
                        courseSession.getId()
                )
                .setStartDateTime(
                        courseSession.getStartDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setEndDateTime(
                        courseSession.getEndDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setBlockContainerCreationBeforeStartTime(
                        courseSession.getBlockContainerCreationBeforeStartTime()
                )
                .setDestroyContainerAfterEndTime(
                        courseSession.getDestroyContainerAfterEndTime()
                )
                .setStudentNumber(
                        courseSession.getStudentNumber()
                );
    }

    public DeploymentSummaryDto convertToSummaryDto(Deployment deployment) {
        DeploymentSummaryDto deploymentDto;
        if (deployment instanceof LaunchDeployment) {
            deploymentDto = new LaunchDeploymentSummaryDto();
        } else if (deployment instanceof CleanDeployment) {
            deploymentDto = new CleanDeploymentSummaryDto();
        } else {
            return null;
        }
        deploymentDto
                .setDescription(
                        deployment.getDescription()
                )
                .setUpdatedOn(
                        deployment.getUpdatedOn().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setId(
                        deployment.getId()
                )
                .setStartDateTime(
                        deployment.getStartDateTime().atZone(zoneId).toInstant().toEpochMilli()
                );
        return deploymentDto;
    }

    public SessionDto convertToDto(CourseSession courseSession) {
        return (SessionDto) new SessionDto()
                .setCleanDeployment(
                        (CleanDeploymentSummaryDto) convertToSummaryDto(courseSession.getCleanDeployment())
                )
                .setLaunchDeployment(
                        (LaunchDeploymentSummaryDto) convertToSummaryDto(courseSession.getLaunchDeployment())
                )
                .setId(
                        courseSession.getId()
                )
                .setStartDateTime(
                        courseSession.getStartDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setEndDateTime(
                        courseSession.getEndDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setBlockContainerCreationBeforeStartTime(
                        courseSession.getBlockContainerCreationBeforeStartTime()
                )
                .setDestroyContainerAfterEndTime(
                        courseSession.getDestroyContainerAfterEndTime()
                )
                .setStudentNumber(
                        courseSession.getStudentNumber()
                )
                ;
    }

    public CourseSession convertToModel(SessionUpdateDto courseSessionDto) {
        CourseSession courseSession;
        if (courseSessionDto.getId() != null) {
            courseSession = courseSessionRepository.getReferenceById(courseSessionDto.getId());
        } else {
            courseSession = new CourseSession();
        }
        return courseSession
                .setStartDateTime(
                        Instant.ofEpochMilli(courseSessionDto.getStartDateTime()).atZone(zoneId).toLocalDateTime()
                )
                .setEndDateTime(
                        Instant.ofEpochMilli(courseSessionDto.getEndDateTime()).atZone(zoneId).toLocalDateTime()
                )
                .setBlockContainerCreationBeforeStartTime(
                        courseSessionDto.getBlockContainerCreationBeforeStartTime()
                )
                .setDestroyContainerAfterEndTime(
                        courseSessionDto.getDestroyContainerAfterEndTime()
                )
                .setStudentNumber(
                        courseSessionDto.getStudentNumber()
                );
    }
}
