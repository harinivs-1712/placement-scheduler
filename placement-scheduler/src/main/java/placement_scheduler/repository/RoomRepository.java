package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.Room;

@Repository
public interface RoomRepository
        extends JpaRepository<Room, Long> {
}
