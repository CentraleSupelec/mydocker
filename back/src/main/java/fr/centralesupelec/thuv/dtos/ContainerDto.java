package fr.centralesupelec.thuv.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ContainerDto {
    @Getter(AccessLevel.NONE)
    private Logger logger = LoggerFactory.getLogger(ContainerDto.class);
    private String username;
    private String password;
    private String ip;
    private List<ContainerPortDto> ports;
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
}
