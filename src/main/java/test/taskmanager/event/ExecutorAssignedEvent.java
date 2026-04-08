package test.taskmanager.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExecutorAssignedEvent {
    private Long taskId;
    private String taskName;
    private Long executorId;
    private String executorName;
    private String executorEmail;
    private LocalDateTime assignedAt;
}