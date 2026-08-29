package placement_scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "replan_runs")
public class ReplanRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replanId;

    @OneToOne
    @JoinColumn(name = "event_id", nullable = false)
    private DisruptionEvent event;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false, length = 20)
    private String status;

    private Integer interviewsAffected;

    private Integer interviewsMoved;

    private Integer interviewsCancelled;

    public ReplanRun() {
    }

    public Long getReplanId() {
        return replanId;
    }

    public void setReplanId(Long replanId) {
        this.replanId = replanId;
    }

    public DisruptionEvent getEvent() {
        return event;
    }

    public void setEvent(DisruptionEvent event) {
        this.event = event;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getInterviewsAffected() {
        return interviewsAffected;
    }

    public void setInterviewsAffected(Integer interviewsAffected) {
        this.interviewsAffected = interviewsAffected;
    }

    public Integer getInterviewsMoved() {
        return interviewsMoved;
    }

    public void setInterviewsMoved(Integer interviewsMoved) {
        this.interviewsMoved = interviewsMoved;
    }

    public Integer getInterviewsCancelled() {
        return interviewsCancelled;
    }

    public void setInterviewsCancelled(Integer interviewsCancelled) {
        this.interviewsCancelled = interviewsCancelled;
    }
}