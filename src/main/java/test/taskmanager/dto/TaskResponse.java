package test.taskmanager.dto;

import lombok.Builder;
import lombok.Data;
import test.taskmanager.model.TaskStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String name;
    private String description;
    private TaskStatus status;
    private ExecutorInfo executor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ExecutorInfo {
        private Long id;
        private String name;
        private String email;
    }
}