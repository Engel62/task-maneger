package test.taskmanager.repository;

import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import test.taskmanager.model.Task;

import java.awt.print.Pageable;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findAll(Pageable pageable);
}
