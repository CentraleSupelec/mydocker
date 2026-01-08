package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.AdminContainerRequest;
import fr.centralesupelec.gRPC.AdminContainerResponse;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.storage.ContainerStorage;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.mappers.GrpcResponsePortToContainerPortDtoMapper;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RequestAdminContainerService {
    private static final Logger logger = LoggerFactory.getLogger(RequestAdminContainerService.class);

    private final ManagedChannel channel;
    private final ContainerStorage containerStorage;
    private StreamObserver<AdminContainerRequest> adminContainerRequestStreamObserver;
    private final ReentrantLock lock = new ReentrantLock();
    private final GrpcResponsePortToContainerPortDtoMapper grpcResponsePortToContainerPortDtoMapper;

    @Autowired
    public RequestAdminContainerService(
            ManagedChannel channel,
            ContainerStorage containerStorage,
            GrpcResponsePortToContainerPortDtoMapper grpcResponsePortToContainerPortDtoMapper
    ) {
        this.channel = channel;
        this.containerStorage = containerStorage;
        this.grpcResponsePortToContainerPortDtoMapper = grpcResponsePortToContainerPortDtoMapper;
    }

    @PostConstruct
    public void init() {
        containerServiceGrpc.containerServiceStub asyncStub = containerServiceGrpc.newStub(channel);
        this.adminContainerRequestStreamObserver = asyncStub.getAdminContainer(new StreamObserver<>() {

            public void onNext(AdminContainerResponse adminContainerResponse) {
                ContainerDto containerDto = mapContainerResponseToContainer(adminContainerResponse);
                containerDto.setStatus(ContainerStatusDto.OK);
                containerStorage.addAdminContainer(
                        containerDto,
                        Long.valueOf(adminContainerResponse.getCourseID())
                );
            }

            public void onError(Throwable throwable) {
                logger.error("Error requesting a container.\n Please restart the go API then this service.");
                if (logger.isDebugEnabled()) {
                    throwable.printStackTrace();
                }
                Sentry.captureMessage("Unable to obtain the container");
            }

            public void onCompleted() {
            }
        });
    }

    public void requestContainer(AdminContainerRequest request) {
        try {
            lock.lock();
            adminContainerRequestStreamObserver.onNext(request);
        } catch (RuntimeException e) {
            // Cancel RPC
            adminContainerRequestStreamObserver.onError(e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private ContainerDto mapContainerResponseToContainer(AdminContainerResponse containerResponse) {
        ContainerDto containerDto = new ContainerDto();
        containerDto.setIp(containerResponse.getIpAddress());
        containerDto.setPorts(
                Collections.singletonList(
                        grpcResponsePortToContainerPortDtoMapper.convertToContainerPortDto(
                                containerResponse.getPort()
                        )
                )
        );

        // Need to change it if different auth method
        containerDto.setPassword(containerResponse.getUserPassword().getPassword());
        containerDto.setUsername(containerResponse.getUserPassword().getUsername());
        containerDto.setNeedsNewGpu(false);
        return containerDto;
    }
}
