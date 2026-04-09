package test.taskmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import test.taskmanager.event.ExecutorAssignedEvent;
import test.taskmanager.event.TaskCreatedEvent;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventService Tests")
class KafkaEventServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaEventService kafkaEventService;

    private TaskCreatedEvent taskCreatedEvent;
    private ExecutorAssignedEvent executorAssignedEvent;

    @BeforeEach
    void setUp() {
        taskCreatedEvent = TaskCreatedEvent.builder()
                .taskId(1L)
                .taskName("Test Task")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .build();

        executorAssignedEvent = ExecutorAssignedEvent.builder()
                .taskId(1L)
                .taskName("Test Task")
                .executorId(1L)
                .executorName("Test Executor")
                .executorEmail("executor@test.com")
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should send TaskCreatedEvent successfully")
    void sendTaskCreatedEvent_Success() throws Exception {
        // Given
        String jsonMessage = "{\"taskId\":1,\"taskName\":\"Test Task\"}";
        when(objectMapper.writeValueAsString(taskCreatedEvent)).thenReturn(jsonMessage);

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("task-created"), eq("1"), eq(jsonMessage))).thenReturn(future);

        // When
        kafkaEventService.sendTaskCreatedEvent(taskCreatedEvent);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(taskCreatedEvent);
        verify(kafkaTemplate, times(1)).send(eq("task-created"), eq("1"), eq(jsonMessage));
    }

    @Test
    @DisplayName("Should handle serialization error for TaskCreatedEvent")
    void sendTaskCreatedEvent_SerializationError() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(taskCreatedEvent)).thenThrow(new RuntimeException("Serialization error"));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            kafkaEventService.sendTaskCreatedEvent(taskCreatedEvent);
        });

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should send ExecutorAssignedEvent successfully")
    void sendExecutorAssignedEvent_Success() throws Exception {
        // Given
        String jsonMessage = "{\"taskId\":1,\"executorId\":1}";
        when(objectMapper.writeValueAsString(executorAssignedEvent)).thenReturn(jsonMessage);

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("executor-assigned"), eq("1"), eq(jsonMessage))).thenReturn(future);

        // When
        kafkaEventService.sendExecutorAssignedEvent(executorAssignedEvent);

        // Then
        verify(objectMapper, times(1)).writeValueAsString(executorAssignedEvent);
        verify(kafkaTemplate, times(1)).send(eq("executor-assigned"), eq("1"), eq(jsonMessage));
    }
}