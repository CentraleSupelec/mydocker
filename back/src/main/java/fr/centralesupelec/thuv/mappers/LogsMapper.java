package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.gRPC.LogResponse;
import fr.centralesupelec.gRPC.TaskLog;
import fr.centralesupelec.thuv.dtos.LogResponseDto;
import fr.centralesupelec.thuv.dtos.TaskLogDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LogsMapper {
    public LogResponseDto convertToDTO(LogResponse logResponse) {
        return new LogResponseDto()
                .setName(logResponse.getName())
                .setImage(logResponse.getImage())
                .setTasks(convertTasks(logResponse.getLogsList()))
                .setOmittedTasks(logResponse.getOmittedTasks());
    }

    /**
     * Renders the same tasks as plain text, one header line per task.
     * Only the retired {@code logs/{courseId}} endpoint uses this, so that a browser still holding
     * the previous front-end bundle keeps seeing all of its output during the deploy window. Delete
     * it together with that endpoint in the release after 2.34.0.
     */
    public String convertToText(LogResponse logResponse) {
        return logResponse.getLogsList().stream()
                .map(LogsMapper::renderTask)
                .collect(Collectors.joining("\n\n"));
    }

    private List<TaskLogDto> convertTasks(List<TaskLog> tasks) {
        return tasks.stream().map(LogsMapper::convertTask).collect(Collectors.toList());
    }

    private static TaskLogDto convertTask(TaskLog task) {
        return new TaskLogDto()
                .setTaskId(task.getTaskID())
                .setSlot(task.getSlot())
                .setNode(task.getNode())
                .setCreatedAt(
                        task.hasCreatedAt()
                                ? Instant.ofEpochSecond(
                                        task.getCreatedAt().getSeconds(),
                                        task.getCreatedAt().getNanos()
                                )
                                : null
                )
                .setLogs(task.getLogs())
                .setTruncated(task.getTruncated())
                .setReadError(task.getReadError());
    }

    private static String renderTask(TaskLog task) {
        String body = task.getReadError().isEmpty() ? task.getLogs() : task.getReadError();
        return String.format("--- %s (slot %d) ---%n%s", task.getNode(), task.getSlot(), body);
    }
}
