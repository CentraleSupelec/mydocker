package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.ContainerRequest;
import fr.centralesupelec.thuv.mappers.PortToGrpcRequestPortMapper;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.StorageBackend;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.PathRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerRequestCreatorServiceRunUidTest {

    private static ContainerRequest createRequest(String runUid) {
        return createRequest(runUid, true);
    }

    private static ContainerRequest createRequest(String runUid, boolean useStudentVolume) {
        PathRepository pathRepository = mock(PathRepository.class);
        when(pathRepository.findAll()).thenReturn(List.of());
        UserCourseRepository userCourseRepository = mock(UserCourseRepository.class);
        when(userCourseRepository.findByUserIdAndCourseId(any(), any())).thenReturn(Optional.empty());
        CourseInfraRequestService courseInfraRequestService = mock(CourseInfraRequestService.class);
        when(courseInfraRequestService.getCourseInfra(anyString())).thenReturn(false);

        ContainerRequestCreatorService service = new ContainerRequestCreatorService(
                mock(PortToGrpcRequestPortMapper.class),
                userCourseRepository,
                mock(ContainerUtilsService.class),
                mock(ComputeTypeRepository.class),
                courseInfraRequestService,
                pathRepository
        );

        ComputeType computeType = new ComputeType();
        computeType.setTechnicalName("gpu-owner");
        computeType.setStorageBackend(StorageBackend.NFS);
        Course course = new Course();
        course.setId(25L);
        course.setTitle("course");
        course.setDockerImage("image");
        course.setCommand("");
        course.setComputeType(computeType);
        course.setUid("57439");
        course.setRunUid(runUid);
        course.setUseStudentVolume(useStudentVolume);
        course.setStudentVolumePath("/home/student");
        User user = new User();
        user.setId(7L);
        user.setUsername("student");

        return service.createRequest(course, user, false, null, null, false);
    }

    @Test
    void sendsRunUidWhenSet() {
        assertEquals("1000:100", createRequest("1000:100").getOptions().getRunUid());
    }

    @Test
    void leavesRunUidUnsetWhenEmptyOrNull() {
        assertEquals("", createRequest(null).getOptions().getRunUid());
        assertEquals("", createRequest("").getOptions().getRunUid());
    }

    @Test
    void leavesRunUidUnsetWhenStudentVolumeIsOff() {
        assertEquals("", createRequest("1000:100", false).getOptions().getRunUid());
    }

    @Test
    void keepsTheOldUidAlongside() {
        assertEquals("57439", createRequest("1000:100").getOptions().getUid());
    }
}
