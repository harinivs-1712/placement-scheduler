package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import placement_scheduler.entity.Shortlist;

public interface ShortlistRepository extends JpaRepository<Shortlist, Long> {
}
