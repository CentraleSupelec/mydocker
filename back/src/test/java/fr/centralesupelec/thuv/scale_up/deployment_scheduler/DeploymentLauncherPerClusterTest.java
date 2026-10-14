package fr.centralesupelec.thuv.scale_up.deployment_scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mail.EmailService;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.scale_up.model.DeploymentStatus;
import fr.centralesupelec.thuv.scale_up.repository.DeploymentRepository;
import fr.centralesupelec.thuv.scale_up.repository.DeploymentStatusRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Each cluster has its own lock: a cluster whose deployment hangs must neither block
// nor skip the other clusters, and only that cluster is skipped on the next run.
class DeploymentLauncherPerClusterTest {
    private final OVHRegionWorkerRepository ovhRegionWorkerRepository = mock(OVHRegionWorkerRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);

    private final DeploymentLauncher launcher = new DeploymentLauncher(
            mock(DeploymentRepository.class),
            ovhRegionWorkerRepository,
            mock(DeploymentStatusRepository.class),
            new ObjectMapper(),
            Map.of(),
            emailService,
            mock(OVHResourceRepository.class),
            mock(OVHRegionRepository.class),
            mock(InitAutoscalingService.class),
            clusterRepository);

    private static Cluster cluster(long id, String name) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        cluster.setName(name);
        cluster.setGoAddress("10.0.0." + id);
        cluster.setGoPort(50000);
        cluster.setDeploymentEnabled(true);
        return cluster;
    }

    private List<?> workersOf(Cluster cluster) {
        return ovhRegionWorkerRepository
                .findOVHRegionWorkerByClusterAndLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentStartDateTimeAfterOrCleanDeploymentIsNull(
                        any(LocalDateTime.class), any(LocalDateTime.class), eq(cluster));
    }

    @Test
    void aHungClusterDoesNotBlockOrSkipTheOthers() throws Exception {
        Cluster hung = cluster(1, "test");
        Cluster failing = cluster(2, "ovh");
        when(clusterRepository.findAll(any(Sort.class))).thenReturn(List.of(hung, failing));

        CountDownLatch hungStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(workersOf(hung)).thenAnswer(invocation -> {
            hungStarted.countDown();
            release.await(30, TimeUnit.SECONDS);
            throw new IllegalStateException("hung cluster released");
        });
        when(workersOf(failing)).thenThrow(new IllegalStateException("ovh unreachable"));

        CompletableFuture<Void> firstRun = CompletableFuture.runAsync(launcher::launchDeployment);
        assertThat(hungStarted.await(10, TimeUnit.SECONDS)).isTrue();

        // The other cluster runs to its end while the hung one still holds its own lock.
        verify(emailService, timeout(10_000)).sendDeploymentErrorMail(any(DeploymentStatus.class), eq(failing));
        verify(emailService, never()).sendDeploymentErrorMail(any(DeploymentStatus.class), eq(hung));

        // A second run skips only the busy cluster and returns without waiting for it.
        CompletableFuture.runAsync(launcher::launchDeployment).get(10, TimeUnit.SECONDS);
        verify(emailService, times(2)).sendDeploymentErrorMail(any(DeploymentStatus.class), eq(failing));
        verify(ovhRegionWorkerRepository, times(1))
                .findOVHRegionWorkerByClusterAndLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentStartDateTimeAfterOrCleanDeploymentIsNull(
                        any(LocalDateTime.class), any(LocalDateTime.class), eq(hung));
        assertThat(firstRun).isNotDone();

        release.countDown();
        firstRun.get(10, TimeUnit.SECONDS);
        verify(emailService).sendDeploymentErrorMail(any(DeploymentStatus.class), eq(hung));
    }
}
