package fr.centralesupelec.thuv.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ContainerDto {
    @Getter(AccessLevel.NONE)
    private Logger logger = LoggerFactory.getLogger(ContainerDto.class);
    @EqualsAndHashCode.Include
    private final String username;
    @EqualsAndHashCode.Include
    private final String password;
    private String ip;
    @EqualsAndHashCode.Include
    private final List<ContainerPortDto> ports;
    @NotNull
    private ContainerStatusDto status = ContainerStatusDto.PENDING;
    public ContainerDto setStatus(ContainerStatusDto status) {
        logger.trace("Setting status to {}", status);
        this.status = status;
        return this;
    }
    private ContainerSwarmStateDto state;

    public ContainerDto setState(ContainerSwarmStateDto state) {
        logger.trace("Setting state to {}", state);
        this.state = state;
        return this;
    }

    private Long deletionTime;
    private Boolean needsNewGpu;
    private String creationError;
    private Map<String, String> errorParams = new HashMap<>();
    @EqualsAndHashCode.Include
    private final LocalDateTime createdAt;
}
