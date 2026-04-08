package test.taskmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignExecutorRequest {
    @NotNull(message = "ID исполнителя обязательно")
    private Long executorId;
}