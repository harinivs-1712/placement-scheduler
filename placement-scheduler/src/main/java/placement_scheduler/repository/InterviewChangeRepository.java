package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.InterviewChange;

import java.util.List;

@Repository
public interface InterviewChangeRepository
        extends JpaRepository<InterviewChange, Long> {

    List<InterviewChange> findByInterviewInterviewId(
            Long interviewId
    );

    List<InterviewChange> findByReplanId(
            Long replanId
    );

}