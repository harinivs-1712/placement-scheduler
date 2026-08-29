package placement_scheduler.dto;

import java.time.LocalTime;

public class ScheduledInterviewDTO {

    private Long interviewId;

    private Integer day;
    private LocalTime startTime;
    private LocalTime endTime;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long companyId;
    private String companyName;

    private Long panelId;
    private String panelName;

    private Long roomId;
    private String roomName;

    private String status;

    public ScheduledInterviewDTO() {
    }

    public ScheduledInterviewDTO(
            Long interviewId,
            Integer day,
            LocalTime startTime,
            LocalTime endTime,
            Long studentId,
            String studentName,
            String studentEmail,
            Long companyId,
            String companyName,
            Long panelId,
            String panelName,
            Long roomId,
            String roomName,
            String status) {

        this.interviewId = interviewId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;

        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;

        this.companyId = companyId;
        this.companyName = companyName;

        this.panelId = panelId;
        this.panelName = panelName;

        this.roomId = roomId;
        this.roomName = roomName;

        this.status = status;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Long getPanelId() {
        return panelId;
    }

    public void setPanelId(Long panelId) {
        this.panelId = panelId;
    }

    public String getPanelName() {
        return panelName;
    }

    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
