package fr.centralesupelec.thuv.scale_up.deployment_scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.gRPC.*;
import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mail.EmailService;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.scale_up.model.*;
import fr.centralesupelec.thuv.scale_up.repository.*;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeploymentLauncher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentLauncher.class);

    private final DeploymentRepository deploymentRepository;
    private final OVHRegionWorkerRepository ovhRegionWorkerRepository;
    private final DeploymentStatusRepository deploymentStatusRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, ManagedChannel> channels;
    private final EmailService emailService;
    private final OVHResourceRepository ovhResourceRepository;
    private final OVHRegionRepository ovhRegionRepository;

    private final InitAutoscalingService initAutoscalingService;
    private final ClusterRepository clusterRepository;

    private final ConcurrentHashMap<String, ReentrantLock> clusterLocks = new ConcurrentHashMap<>();
    private final ExecutorService deploymentExecutor = Executors.newFixedThreadPool(10);

    private static GrpcWorkerDto mapToGrpcDto(OVHRegionWorker ovhRegionWorker) {
        String[] courseIds = ovhRegionWorker.getLaunchDeployment().getSessionsToLaunch()
                .stream()
                .map(session -> String.valueOf(session.getCourse().getId()))
                .collect(Collectors.toSet())
                .toArray(new String[0]);
        logger.info("Course Ids linked to ovh worker : {}", Arrays.toString(courseIds));

        return new GrpcWorkerDto()
                .setCount(ovhRegionWorker.getCount())
                .setFlavor(ovhRegionWorker.getRessource().getType())
                .setImageId(ovhRegionWorker.getRegion().getImageId())
                .setRegion(ovhRegionWorker.getRegion().getRegion())
                .setOwner(
                        ovhRegionWorker.getComputeType() == null
                                ? null
                                : ovhRegionWorker.getComputeType().getTechnicalName())
                .setCourseIds(courseIds);
    }

    public void launchDeployment() {
        List<Cluster> clusters = clusterRepository.findAll(Sort.by("id"));

        List<CompletableFuture<Void>> futures = clusters.stream()
                .filter(Cluster::getDeploymentEnabled)
                .map(cluster -> CompletableFuture.runAsync(() -> deploySingleCluster(cluster), deploymentExecutor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void deploySingleCluster(Cluster cluster) {
        ReentrantLock clusterLock = clusterLocks.computeIfAbsent(cluster.getName(), k -> new ReentrantLock());

        if (!clusterLock.tryLock()) {
            logger.warn(String.format("A deployment is already running for cluster %s...", cluster.getName()));
            return;
        }

        DeploymentStatus deploymentStatus = new DeploymentStatus();
        deploymentStatus.setCluster(cluster);
        List<Deployment> deployments = Collections.emptyList();

        try {
            logger.info(String.format("Starting deployment for cluster %s ...", cluster.getName()));
            LocalDateTime now = LocalDateTime.now();

            List<OVHRegionWorker> ovhRegionWorkers = ovhRegionWorkerRepository
                    .findOVHRegionWorkerByClusterAndLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentStartDateTimeAfterOrCleanDeploymentIsNull(
                            now, now, cluster);

            deployments = deploymentRepository
                    .findByStartDateTimeBeforeAndStatusAndClusterOrderByStartDateTimeDesc(
                            now, Deployment.Status.CREATED, cluster);

            deploymentStatus.setStatus(DeploymentStatus.Status.CREATED)
                    .setWorkers(objectMapper.writeValueAsString(
                            ovhRegionWorkers.stream()
                                    .map(DeploymentLauncher::mapToGrpcDto)
                                    .collect(Collectors.toList())));
            deploymentStatusRepository.saveAndFlush(deploymentStatus);

            DeployRequest request = DeployRequest.newBuilder()
                    .setId(deploymentStatus.getId().toString())
                    .addAllCourseIds(deployments.stream()
                            .filter(CleanDeployment.class::isInstance)
                            .map(CleanDeployment.class::cast)
                            .flatMap(d -> d.getSessionsToClean().stream())
                            .map(session -> session.getCourse().getId().toString())
                            .collect(Collectors.toList()))
                    .addAllWorkers(ovhRegionWorkers.stream().map(this::convertToWorker).collect(Collectors.toList()))
                    .addAllWorkers(getDefaultWorkers())
                    .build();

            containerServiceGrpc.containerServiceBlockingStub stub =
                    containerServiceGrpc.newBlockingStub(channels.get(cluster.getName()));

            for (Iterator<DeployResponse> i = stub.deployInfra(request); i.hasNext(); ) {
                DeployResponse r = i.next();
                DeploymentStatus.Status status = convert(r.getStatus());
                deploymentStatus.setStatus(status)
                        .setLogs(r.getLogs())
                        .setBuildErrors(r.getError());
                deploymentStatusRepository.saveAndFlush(deploymentStatus);
                updateDeployments(deployments, status);
            }

            initAutoscalingService.initAutoscalingByCluster(cluster);
        } catch (Exception e) {
            deploymentStatus.setStatus(DeploymentStatus.Status.ERROR)
                    .setBuildErrors(String.format("Failed to deploy infra on cluster %s: %s", cluster.getName(), e.getMessage()));
            deploymentStatusRepository.saveAndFlush(deploymentStatus);
            logger.error(String.format("Failed to deploy infra on cluster %s: %s", cluster.getName(), e.getMessage()), e);
            updateDeployments(deployments, DeploymentStatus.Status.ERROR);
        } finally {
            clusterLock.unlock();
        }

        if (DeploymentStatus.Status.ERROR.equals(deploymentStatus.getStatus())) {
            emailService.sendDeploymentErrorMail(deploymentStatus, cluster);
        }
    }

    private List<Worker> getDefaultWorkers() {
        Optional<OVHResource> defaultOVHResource = ovhResourceRepository.findAll().stream().findFirst();
        return defaultOVHResource.map(ovhResource -> ovhRegionRepository
                .findAll()
                .stream()
                .map(ovhRegion -> Worker.newBuilder()
                        .setCount(
                                0)
                        .setFlavor(
                                ovhResource.getType())
                        .setImageId(
                                ovhRegion.getImageId())
                        .setRegion(
                                ovhRegion.getRegion())
                        .build())
                .collect(Collectors.toList())).orElseGet(List::of);
    }

    private Worker convertToWorker(OVHRegionWorker ovhRegionWorker) {
        Worker.Builder workerBuilder = Worker.newBuilder()
                .setCount(
                        ovhRegionWorker.getCount().intValue())
                .setFlavor(
                        ovhRegionWorker.getRessource().getType())
                .setImageId(
                        ovhRegionWorker.getRegion().getImageId())
                .setRegion(
                        ovhRegionWorker.getRegion().getRegion())
                .addAllCourseIds(ovhRegionWorker.getLaunchDeployment().getSessionsToLaunch()
                        .stream().map(session -> session.getCourse().getId().toString()).collect(Collectors.toSet()));
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
                    });
        }

        if (status.equals(DeploymentStatus.Status.ERROR) || status.equals(DeploymentStatus.Status.SKIPPED)) {
            deployments.forEach(
                    d -> {
                        d.setStatus(Deployment.Status.CREATED);
                        deploymentRepository.saveAndFlush(d);
                    });
        }

        if (status.equals(DeploymentStatus.Status.DONE)) {
            deployments.forEach(
                    d -> {
                        d.setStatus(Deployment.Status.OK);
                        deploymentRepository.saveAndFlush(d);
                    });
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        this.launchDeployment();
    }
}
