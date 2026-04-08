package test.taskmanager.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskCreatedEvent {
    private Long taskId;
    private String taskName;
    private String description;
    private LocalDateTime createdAt;
}