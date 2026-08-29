package placement_scheduler.controller;

import java.time.LocalTime;
import java.util.List;

public class ReplanDetailsResponse {

    private Long replanId;
    private String disruptionType;

    private Integer affectedCount;
    private Integer movedCount;
    private Integer failedCount;

    private List<InterviewChangeResponse> successfulChanges;
    private List<FailedInterviewResponse> failedInterviews;

    public ReplanDetailsResponse() {
    }

    public Long getReplanId() {
        return replanId;
    }

    public void setReplanId(Long replanId) {
        this.replanId = replanId;
    }

    public String getDisruptionType() {
        return disruptionType;
    }

    public void setDisruptionType(String disruptionType) {
        this.disruptionType = disruptionType;
    }

    public Integer getAffectedCount() {
        return affectedCount;
    }

    public void setAffectedCount(Integer affectedCount) {
        this.affectedCount = affectedCount;
    }

    public Integer getMovedCount() {
        return movedCount;
    }

    public void setMovedCount(Integer movedCount) {
        this.movedCount = movedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<InterviewChangeResponse> getSuccessfulChanges() {
        return successfulChanges;
    }

    public void setSuccessfulChanges(
            List<InterviewChangeResponse> successfulChanges) {
        this.successfulChanges = successfulChanges;
    }

    public List<FailedInterviewResponse> getFailedInterviews() {
        return failedInterviews;
    }

    public void setFailedInterviews(
            List<FailedInterviewResponse> failedInterviews) {
        this.failedInterviews = failedInterviews;
    }

    public static class InterviewChangeResponse {

        private Long interviewId;
        private Long studentId;
        private Long companyId;

        private Integer oldDay;
        private LocalTime oldStartTime;
        private LocalTime oldEndTime;
        private Long oldPanelId;
        private Long oldRoomId;

        private Integer newDay;
        private LocalTime newStartTime;
        private LocalTime newEndTime;
        private Long newPanelId;
        private Long newRoomId;

        public InterviewChangeResponse() {
        }

        public Long getInterviewId() {
            return interviewId;
        }

        public void setInterviewId(Long interviewId) {
            this.interviewId = interviewId;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public void setCompanyId(Long companyId) {
            this.companyId = companyId;
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
    }

    public static class FailedInterviewResponse {

        private Long interviewId;
        private Long studentId;
        private Long companyId;
        private String reason;

        public FailedInterviewResponse() {
        }

        public Long getInterviewId() {
            return interviewId;
        }

        public void setInterviewId(Long interviewId) {
            this.interviewId = interviewId;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public void setCompanyId(Long companyId) {
            this.companyId = companyId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}