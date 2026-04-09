package test.taskmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import test.taskmanager.dto.AssignExecutorRequest;
import test.taskmanager.dto.StatusUpdateRequest;
import test.taskmanager.dto.TaskRequest;
import test.taskmanager.dto.TaskResponse;
import test.taskmanager.event.ExecutorAssignedEvent;
import test.taskmanager.event.TaskCreatedEvent;
import test.taskmanager.exeption.TaskNotFoundException;
import test.taskmanager.exeption.UserNotFoundException;
import test.taskmanager.model.Task;
import test.taskmanager.model.TaskStatus;
import test.taskmanager.model.User;
import test.taskmanager.repository.TaskRepository;
import test.taskmanager.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaEventService kafkaEventService;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private User testUser;
    private TaskRequest taskRequest;
    private AssignExecutorRequest assignRequest;
    private StatusUpdateRequest statusRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testTask = new Task();
        testTask.setId(1L);
        testTask.setName("Test Task");
        testTask.setDescription("Test Description");
        testTask.setStatus(TaskStatus.CREATED);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());

        taskRequest = new TaskRequest();
        taskRequest.setName("New Task");
        taskRequest.setDescription("New Description");

        assignRequest = new AssignExecutorRequest();
        assignRequest.setExecutorId(1L);

        statusRequest = new StatusUpdateRequest();
        statusRequest.setStatus("IN_PROGRESS");
    }

    @Test
    @DisplayName("Should create task successfully")
    void createTask_Success() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        TaskResponse response = taskService.createTask(taskRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Task");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.CREATED);

        verify(taskRepository, times(1)).save(any(Task.class));
        verify(kafkaEventService, times(1)).sendTaskCreatedEvent(any(TaskCreatedEvent.class));
    }

    @Test
    @DisplayName("Should get task by id successfully")
    void getTaskById_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When
        TaskResponse response = taskService.getTaskById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Task");

        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when task not found by id")
    void getTaskById_NotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");

        verify(taskRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get all tasks with pagination")
    void getAllTasks_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);
        when(taskRepository.findAll(pageable)).thenReturn(taskPage);

        // When
        Page<TaskResponse> response = taskService.getAllTasks(pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Test Task");

        verify(taskRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should assign executor to task successfully")
    void assignExecutor_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        TaskResponse response = taskService.assignExecutor(1L, assignRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExecutor()).isNotNull();
        assertThat(response.getExecutor().getId()).isEqualTo(1L);
        assertThat(response.getExecutor().getName()).isEqualTo("Test User");

        verify(taskRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(kafkaEventService, times(1)).sendExecutorAssignedEvent(any(ExecutorAssignedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when task not found for assign executor")
    void assignExecutor_TaskNotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.assignExecutor(999L, assignRequest))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");

        verify(taskRepository, times(1)).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(kafkaEventService, never()).sendExecutorAssignedEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found for assign executor")
    void assignExecutor_UserNotFound() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assignRequest.setExecutorId(999L);

        // When & Then
        assertThatThrownBy(() -> taskService.assignExecutor(1L, assignRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(taskRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(999L);
        verify(kafkaEventService, never()).sendExecutorAssignedEvent(any());
    }

    @Test
    @DisplayName("Should update task status successfully")
    void updateStatus_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        TaskResponse response = taskService.updateStatus(1L, statusRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when task not found for status update")
    void updateStatus_TaskNotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.updateStatus(999L, statusRequest))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");

        verify(taskRepository, times(1)).findById(999L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when invalid status provided")
    void updateStatus_InvalidStatus() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        statusRequest.setStatus("INVALID_STATUS");

        // When & Then
        assertThatThrownBy(() -> taskService.updateStatus(1L, statusRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status value");

        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should verify Kafka event is sent on task creation")
    void createTask_SendsKafkaEvent() {
        // Given
        ArgumentCaptor<TaskCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCreatedEvent.class);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        taskService.createTask(taskRequest);

        // Then
        verify(kafkaEventService).sendTaskCreatedEvent(eventCaptor.capture());
        TaskCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTaskId()).isEqualTo(1L);
        assertThat(capturedEvent.getTaskName()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("Should verify Kafka event is sent on executor assignment")
    void assignExecutor_SendsKafkaEvent() {
        // Given
        ArgumentCaptor<ExecutorAssignedEvent> eventCaptor = ArgumentCaptor.forClass(ExecutorAssignedEvent.class);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        taskService.assignExecutor(1L, assignRequest);

        // Then
        verify(kafkaEventService).sendExecutorAssignedEvent(eventCaptor.capture());
        ExecutorAssignedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTaskId()).isEqualTo(1L);
        assertThat(capturedEvent.getExecutorId()).isEqualTo(1L);
        assertThat(capturedEvent.getExecutorName()).isEqualTo("Test User");
    }
}