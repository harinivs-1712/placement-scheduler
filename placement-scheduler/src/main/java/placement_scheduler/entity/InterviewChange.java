package placement_scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "interview_changes")
public class InterviewChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long changeId;

    @ManyToOne
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(name = "old_day")
    private Integer oldDay;

    @Column(name = "old_start_time")
    private LocalTime oldStartTime;

    @Column(name = "old_end_time")
    private LocalTime oldEndTime;

    @Column(name = "old_panel_id")
    private Long oldPanelId;

    @Column(name = "old_room_id")
    private Long oldRoomId;

    @Column(name = "new_day")
    private Integer newDay;

    @Column(name = "new_start_time")
    private LocalTime newStartTime;

    @Column(name = "new_end_time")
    private LocalTime newEndTime;

    @Column(name = "new_panel_id")
    private Long newPanelId;

    @Column(name = "new_room_id")
    private Long newRoomId;

    @Column(name = "change_type", nullable = false, length = 100)
    private String changeType;

    @Column(name = "replan_id")
    private Long replanId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public InterviewChange() {
    }

    public Long getChangeId() {
        return changeId;
    }

    public void setChangeId(Long changeId) {
        this.changeId = changeId;
    }

    public Interview getInterview() {
        return interview;
    }

    public void setInterview(Interview interview) {
        this.interview = interview;
    }

    public Integer getOldDay() {
        return oldDay;
    }

    public void setOldDay(Integer oldDay) {
        this.oldDay = oldDay;
    }

    public LocalTime getOldStartTime() {
        return oldStartTime;
    }

    public void setOldStartTime(LocalTime oldStartTime) {
        this.oldStartTime = oldStartTime;
    }

    public LocalTime getOldEndTime() {
        return oldEndTime;
    }

    public void setOldEndTime(LocalTime oldEndTime) {
        this.oldEndTime = oldEndTime;
    }

    public Long getOldPanelId() {
        return oldPanelId;
    }

    public void setOldPanelId(Long oldPanelId) {
        this.oldPanelId = oldPanelId;
    }

    public Long getOldRoomId() {
        return oldRoomId;
    }

    public void setOldRoomId(Long oldRoomId) {
        this.oldRoomId = oldRoomId;
    }

    public Integer getNewDay() {
        return newDay;
    }

    public void setNewDay(Integer newDay) {
        this.newDay = newDay;
    }

    public LocalTime getNewStartTime() {
        return newStartTime;
    }

    public void setNewStartTime(LocalTime newStartTime) {
        this.newStartTime = newStartTime;
    }

    public LocalTime getNewEndTime() {
        return newEndTime;
    }

    public void setNewEndTime(LocalTime newEndTime) {
        this.newEndTime = newEndTime;
    }

    public Long getNewPanelId() {
        return newPanelId;
    }

    public void setNewPanelId(Long newPanelId) {
        this.newPanelId = newPanelId;
    }

    public Long getNewRoomId() {
        return newRoomId;
    }

    public void setNewRoomId(Long newRoomId) {
        this.newRoomId = newRoomId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Long getReplanId() {
        return replanId;
    }

    public void setReplanId(Long replanId) {
        this.replanId = replanId;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
