package test.taskmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final KafkaEventService kafkaEventService;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       KafkaEventService kafkaEventService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.kafkaEventService = kafkaEventService;
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        log.info("Creating new task: {}", request.getName());

        Task task = new Task();
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.CREATED);

        Task savedTask = taskRepository.save(task);
        log.info("Task created with id: {}", savedTask.getId());

        TaskCreatedEvent event = TaskCreatedEvent.builder()
                .taskId(savedTask.getId())
                .taskName(savedTask.getName())
                .description(savedTask.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        kafkaEventService.sendTaskCreatedEvent(event);

        return mapToResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public TaskResponse assignExecutor(Long taskId, AssignExecutorRequest request) {
        log.info("Assigning executor {} to task {}", request.getExecutorId(), taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + taskId));

        User executor = userRepository.findById(request.getExecutorId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + request.getExecutorId()));

        User oldExecutor = task.getExecutor();
        task.setExecutor(executor);
        Task updatedTask = taskRepository.save(task);

        log.info("Executor assigned: taskId={}, oldExecutorId={}, newExecutorId={}",
                taskId, oldExecutor != null ? oldExecutor.getId() : null, executor.getId());

        ExecutorAssignedEvent event = ExecutorAssignedEvent.builder()
                .taskId(updatedTask.getId())
                .taskName(updatedTask.getName())
                .executorId(executor.getId())
                .executorName(executor.getName())
                .executorEmail(executor.getEmail())
                .assignedAt(LocalDateTime.now())
                .build();
        kafkaEventService.sendExecutorAssignedEvent(event);

        return mapToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateStatus(Long taskId, StatusUpdateRequest request) {
        log.info("Updating status of task {} to {}", taskId, request.getStatus());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + taskId));

        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus;

        try {
            newStatus = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value. Allowed: CREATED, IN_PROGRESS, COMPLETED, CANCELLED");
        }

        task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);

        log.info("Status updated: taskId={}, oldStatus={}, newStatus={}", taskId, oldStatus, newStatus);

        return mapToResponse(updatedTask);
    }

    private TaskResponse mapToResponse(Task task) {
        TaskResponse.TaskResponseBuilder builder = TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt());

        if (task.getExecutor() != null) {
            builder.executor(TaskResponse.ExecutorInfo.builder()
                    .id(task.getExecutor().getId())
                    .name(task.getExecutor().getName())
                    .email(task.getExecutor().getEmail())
                    .build());
        }

        return builder.build();
    }
}