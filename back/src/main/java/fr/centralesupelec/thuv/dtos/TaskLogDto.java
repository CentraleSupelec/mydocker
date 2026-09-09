package fr.centralesupelec.thuv.dtos;

import lombok.Data;

import java.time.Instant;

@Data
public class TaskLogDto {
    private String taskId;
    private long slot;
    private String node;
    private Instant createdAt;
    private String logs;
    private boolean truncated;
    private String readError;
}
