package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import placement_scheduler.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
