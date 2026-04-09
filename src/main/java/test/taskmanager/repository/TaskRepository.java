package test.taskmanager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import test.taskmanager.model.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findAll(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.executor.id = :executorId")
    long countByExecutorId(@Param("executorId") Long executorId);
}
