package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.DisruptionEvent;

import java.util.List;

@Repository
public interface DisruptionEventRepository
        extends JpaRepository<DisruptionEvent, Long> {

    List<DisruptionEvent> findByEventType(String eventType);

    List<DisruptionEvent> findByTargetTypeAndTargetId(
            String targetType,
            Long targetId
    );
}