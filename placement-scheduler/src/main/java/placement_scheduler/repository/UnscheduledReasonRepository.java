package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.UnscheduledReason;

import java.util.List;

@Repository
public interface UnscheduledReasonRepository
        extends JpaRepository<UnscheduledReason, Long> {

    List<UnscheduledReason> findByInterviewInterviewId(
            Long interviewId
    );

    List<UnscheduledReason> findByReplanId(Long replanId);
}
