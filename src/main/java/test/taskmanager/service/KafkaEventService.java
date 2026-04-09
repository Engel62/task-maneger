package test.taskmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import test.taskmanager.event.ExecutorAssignedEvent;
import test.taskmanager.event.TaskCreatedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendTaskCreatedEvent(TaskCreatedEvent event) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("task-created", event.getTaskId().toString(), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("TaskCreatedEvent sent: taskId={}, offset={}, partition={}",
                                    event.getTaskId(),
                                    result.getRecordMetadata().offset(),
                                    result.getRecordMetadata().partition());
                        } else {
                            log.error("Failed to send TaskCreatedEvent for task: {}", event.getTaskId(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TaskCreatedEvent", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public void sendExecutorAssignedEvent(ExecutorAssignedEvent event) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("executor-assigned", event.getTaskId().toString(), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("ExecutorAssignedEvent sent: taskId={}, executorId={}, offset={}",
                                    event.getTaskId(),
                                    event.getExecutorId(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send ExecutorAssignedEvent for task: {}", event.getTaskId(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ExecutorAssignedEvent", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}