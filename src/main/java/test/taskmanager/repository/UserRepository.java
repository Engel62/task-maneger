package test.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import test.taskmanager.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
}
