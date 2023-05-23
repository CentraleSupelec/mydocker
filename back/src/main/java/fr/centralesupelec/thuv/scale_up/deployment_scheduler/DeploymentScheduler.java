package fr.centralesupelec.thuv.scale_up.deployment_scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.gRPC.*;
import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mail.EmailService;
import fr.centralesupelec.thuv.scale_up.model.*;
import fr.centralesupelec.thuv.scale_up.repository.*;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeploymentScheduler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentScheduler.class);

    private final DeploymentRepository deploymentRepository;
    private final OVHRegionWorkerRepository ovhRegionWorkerRepository;
    private final DeploymentStatusRepository deploymentStatusRepository;
    private final ObjectMapper objectMapper;
    private final ManagedChannel channel;
    private final EmailService emailService;
    private final OVHResourceRepository ovhResourceRepository;
    private final OVHRegionRepository ovhRegionRepository;
    private final ReentrantLock lock = new ReentrantLock();

    private final InitAutoscalingService initAutoscalingService;

    private static GrpcWorkerDto mapToGrpcDto(OVHRegionWorker ovhRegionWorker) {
        return new GrpcWorkerDto()
                .setCount(ovhRegionWorker.getCount())
                .setFlavor(ovhRegionWorker.getRessource().getType())
                .setImageId(ovhRegionWorker.getRegion().getImageId())
                .setRegion(ovhRegionWorker.getRegion().getRegion())
                .setOwner(
                        ovhRegionWorker.getComputeType() == null
                                ? null
                                : ovhRegionWorker.getComputeType().getTechnicalName()
                );
    }

    @Scheduled(fixedDelayString = "${deployment_delay_in_milliseconds}", initialDelay = 0)
    public void launchDeployment() throws JsonProcessingException {
        if (!lock.tryLock()) {
            logger.warn("A deployment is already running ...");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        // Get worker region to launch / clean
        List<OVHRegionWorker> ovhRegionWorkers =
                ovhRegionWorkerRepository
                        .findOVHRegionWorkerByLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentStartDateTimeAfter(
                                now, now
                        );
        ovhRegionWorkers.addAll(
                ovhRegionWorkerRepository
                    .findOVHRegionWorkerByLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentIsNull(
                            now
                    )
        );

        // Find last deployment
        List<Deployment> deployments = deploymentRepository.findByStartDateTimeBeforeAndStatusOrderByStartDateTimeDesc(
                now, Deployment.Status.CREATED
        );

        // Create deploy status
        DeploymentStatus deploymentStatus = new DeploymentStatus()
                .setStatus(DeploymentStatus.Status.CREATED)
                .setWorkers(
                        objectMapper.writeValueAsString(
                                ovhRegionWorkers.stream()
                                        .map(DeploymentScheduler::mapToGrpcDto)
                                        .collect(Collectors.toList())
                        )
                );
        deploymentStatusRepository.saveAndFlush(deploymentStatus);

        List<Worker> defaultWorkers = getDefaultWorkers();

        // Build RPC request
        DeployRequest request = DeployRequest.newBuilder()
                .setId(
                        deploymentStatus.getId().toString()
                )
                .addAllCourseIds(
                        deployments.stream()
                                .filter(deployment -> deployment instanceof CleanDeployment)
                                .flatMap(deployment -> ((CleanDeployment) deployment).getSessionsToClean().stream())
                                .map(courseSession -> courseSession.getCourse().getId().toString())
                                .collect(Collectors.toList())
                )
                .addAllWorkers(
                        ovhRegionWorkers.stream()
                            .map(this::convertToWorker)
                            .collect(Collectors.toList())
                )
                .addAllWorkers(
                        defaultWorkers
                )
                .build();

        // Send RPC request
        containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(channel);

        try {
            for (Iterator<DeployResponse> i = stub.deployInfra(request); i.hasNext(); ) {
                DeployResponse r = i.next();
                DeploymentStatus.Status status = convert(
                        r.getStatus()
                );
                deploymentStatus.setStatus(
                        status
                ).setLogs(
                        r.getLogs()
                ).setBuildErrors(
                        r.getError()
                );
                deploymentStatusRepository.saveAndFlush(deploymentStatus);
                updateDeployments(deployments, status);
            }
            initAutoscalingService.initAutoscaling();
        } catch (Exception e) {
            deploymentStatus.setStatus(
                    DeploymentStatus.Status.ERROR
            ).setBuildErrors(
                    "Failed to deploy infra: " + e.getMessage()
            );
            deploymentStatusRepository.saveAndFlush(deploymentStatus);
            logger.error("Failed to deploy infra: " + e.getMessage(), e);
            updateDeployments(deployments, DeploymentStatus.Status.ERROR);
        } finally {
            lock.unlock();
        }

        if (deploymentStatus.getStatus().equals(DeploymentStatus.Status.ERROR)) {
            emailService.sendDeploymentErrorMail(deploymentStatus);
        }
    }

    private List<Worker> getDefaultWorkers() {
        Optional<OVHResource> defaultOVHResource = ovhResourceRepository.findAll().stream().findFirst();
        return defaultOVHResource.map(ovhResource -> ovhRegionRepository
                .findAll()
                .stream()
                .map(ovhRegion -> Worker.newBuilder()
                        .setCount(
                                0
                        )
                        .setFlavor(
                                ovhResource.getType()
                        )
                        .setImageId(
                                ovhRegion.getImageId()
                        )
                        .setRegion(
                                ovhRegion.getRegion()
                        )
                        .build()
                )
                .collect(Collectors.toList())).orElseGet(List::of);
    }

    private Worker convertToWorker(OVHRegionWorker ovhRegionWorker) {
        Worker.Builder workerBuilder = Worker.newBuilder()
                .setCount(
                        ovhRegionWorker.getCount().intValue()
                )
                .setFlavor(
                        ovhRegionWorker.getRessource().getType()
                )
                .setImageId(
                        ovhRegionWorker.getRegion().getImageId()
                )
                .setRegion(
                        ovhRegionWorker.getRegion().getRegion()
                );
        if (ovhRegionWorker.getComputeType() != null) {
            workerBuilder.setOwner(ovhRegionWorker.getComputeType().getTechnicalName());
        }
        return workerBuilder.build();
    }

    private DeploymentStatus.Status convert(DeployResponse.Status status) {
        switch (status) {
            case SKIPPED:
                return DeploymentStatus.Status.SKIPPED;
            case RUNNING:
                return DeploymentStatus.Status.RUNNING;
            case DONE:
                return DeploymentStatus.Status.DONE;
            default:
                return DeploymentStatus.Status.ERROR;
        }
    }

    private void updateDeployments(List<Deployment> deployments, DeploymentStatus.Status status) {
        if (status.equals(DeploymentStatus.Status.RUNNING)) {
            deployments.forEach(
                    d -> {
                        if (d.getStatus().equals(Deployment.Status.RUNNING)) {
                            return;
                        }
                        d.setStatus(Deployment.Status.RUNNING);
                        deploymentRepository.saveAndFlush(d);
                    }
            );
        }

        if (status.equals(DeploymentStatus.Status.ERROR) || status.equals(DeploymentStatus.Status.SKIPPED)) {
            deployments.forEach(
                    d -> {
                        d.setStatus(Deployment.Status.CREATED);
                        deploymentRepository.saveAndFlush(d);
                    }
            );
        }

        if (status.equals(DeploymentStatus.Status.DONE)) {
            deployments.forEach(
                    d -> {
                        d.setStatus(Deployment.Status.OK);
                        deploymentRepository.saveAndFlush(d);
                    }
            );
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        this.launchDeployment();
    }
}
