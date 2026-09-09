package fr.centralesupelec.thuv.mappers;

import com.google.protobuf.Timestamp;
import fr.centralesupelec.gRPC.LogResponse;
import fr.centralesupelec.gRPC.TaskLog;
import fr.centralesupelec.thuv.dtos.LogResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LogsMapperTest {

    private final LogsMapper mapper = new LogsMapper();

    private static TaskLog task(String id, int slot, String node, long epochSecond, String logs, boolean truncated) {
        return TaskLog.newBuilder()
                .setTaskID(id)
                .setSlot(slot)
                .setNode(node)
                .setCreatedAt(Timestamp.newBuilder().setSeconds(epochSecond).build())
                .setLogs(logs)
                .setTruncated(truncated)
                .build();
    }

    @Test
    void carriesEveryFieldOfEveryTask() {
        LogResponse response = LogResponse.newBuilder()
                .setName("42-7")
                .setImage("harbor.example.org/mydocker/lab:1.2.3")
                .addLogs(task("task-a", 1, "worker-01", 1_700_000_000L, "first attempt", false))
                .addLogs(task("task-b", 2, "worker-02", 1_700_000_060L, "second attempt", true))
                .build();

        LogResponseDto dto = mapper.convertToDTO(response);

        assertThat(dto.getName()).isEqualTo("42-7");
        // The registry host is left alone here; hiding it belongs to the front end.
        assertThat(dto.getImage()).isEqualTo("harbor.example.org/mydocker/lab:1.2.3");
        assertThat(dto.getTasks()).hasSize(2);
        assertThat(dto.getTasks().get(0).getTaskId()).isEqualTo("task-a");
        assertThat(dto.getTasks().get(0).getSlot()).isEqualTo(1);
        assertThat(dto.getTasks().get(0).getNode()).isEqualTo("worker-01");
        assertThat(dto.getTasks().get(0).getCreatedAt()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
        assertThat(dto.getTasks().get(0).getLogs()).isEqualTo("first attempt");
        assertThat(dto.getTasks().get(0).isTruncated()).isFalse();
        assertThat(dto.getTasks().get(1).isTruncated()).isTrue();
    }

    @Test
    void keepsTheOrderTheGoApiSent() {
        LogResponse response = LogResponse.newBuilder()
                .addLogs(task("slot-1-first", 1, "worker-01", 1_700_000_000L, "a", false))
                .addLogs(task("slot-1-retry", 1, "worker-02", 1_700_000_060L, "b", false))
                .addLogs(task("slot-2", 2, "worker-02", 1_700_000_120L, "c", false))
                .build();

        LogResponseDto dto = mapper.convertToDTO(response);

        assertThat(dto.getTasks())
                .extracting("taskId")
                .containsExactly("slot-1-first", "slot-1-retry", "slot-2");
    }

    @Test
    void carriesAReadErrorAndTheOmittedCount() {
        LogResponse response = LogResponse.newBuilder()
                .setOmittedTasks(3)
                .addLogs(TaskLog.newBuilder()
                        .setTaskID("task-broken")
                        .setSlot(1)
                        .setNode("worker-01")
                        .setReadError("no such task")
                        .build())
                .build();

        LogResponseDto dto = mapper.convertToDTO(response);

        assertThat(dto.getOmittedTasks()).isEqualTo(3);
        // Unreadable and empty must stay distinguishable all the way to the browser.
        assertThat(dto.getTasks().get(0).getReadError()).isEqualTo("no such task");
        assertThat(dto.getTasks().get(0).getLogs()).isEmpty();
    }

    @Test
    void theRetiredTextEndpointShowsTheReasonWhenATaskCouldNotBeRead() {
        LogResponse response = LogResponse.newBuilder()
                .addLogs(TaskLog.newBuilder()
                        .setTaskID("task-broken")
                        .setSlot(2)
                        .setNode("worker-02")
                        .setReadError("no such task")
                        .build())
                .build();

        assertThat(mapper.convertToText(response))
                .isEqualTo(String.format("--- worker-02 (slot 2) ---%nno such task"));
    }

    @Test
    void anEmptyResponseMapsToAnEmptyListRatherThanNull() {
        // This is what a version mismatch looks like on the wire: field 1 is reserved, so an older
        // Go API sends nothing this side can read, and the student sees no logs rather than a
        // plausible wrong answer.
        LogResponseDto dto = mapper.convertToDTO(LogResponse.newBuilder().build());

        assertThat(dto.getTasks()).isEmpty();
        assertThat(dto.getName()).isEmpty();
        assertThat(dto.getImage()).isEmpty();
        assertThat(dto.getOmittedTasks()).isZero();
    }

    @Test
    void aTaskWithoutATimestampMapsToNullRatherThanTheEpoch() {
        LogResponse response = LogResponse.newBuilder()
                .addLogs(TaskLog.newBuilder().setTaskID("task-a").setLogs("output").build())
                .build();

        LogResponseDto dto = mapper.convertToDTO(response);

        assertThat(dto.getTasks().get(0).getCreatedAt()).isNull();
    }

    @Test
    void theRetiredTextEndpointRendersOneHeaderPerTask() {
        LogResponse response = LogResponse.newBuilder()
                .addLogs(task("task-a", 1, "worker-01", 1_700_000_000L, "first attempt", false))
                .addLogs(task("task-b", 2, "worker-02", 1_700_000_060L, "second attempt", false))
                .build();

        String text = mapper.convertToText(response);

        assertThat(text).isEqualTo(String.format(
                "--- worker-01 (slot 1) ---%nfirst attempt%n%n--- worker-02 (slot 2) ---%nsecond attempt"
        ));
    }

    @Test
    void theRetiredTextEndpointReturnsEmptyTextWhenThereAreNoTasks() {
        assertThat(mapper.convertToText(LogResponse.newBuilder().build())).isEmpty();
    }
}
