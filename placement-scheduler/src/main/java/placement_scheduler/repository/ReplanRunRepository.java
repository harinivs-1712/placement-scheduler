package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.ReplanRun;

@Repository
public interface ReplanRunRepository
        extends JpaRepository<ReplanRun, Long> {
}