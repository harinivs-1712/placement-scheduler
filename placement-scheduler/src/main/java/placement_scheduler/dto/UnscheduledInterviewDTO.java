package placement_scheduler.dto;

public class UnscheduledInterviewDTO {

    private Long interviewId;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long companyId;
    private String companyName;

    private String status;
    private String reason;

    public UnscheduledInterviewDTO() {
    }

    public UnscheduledInterviewDTO(
            Long interviewId,
            Long studentId,
            String studentName,
            String studentEmail,
            Long companyId,
            String companyName,
            String status,
            String reason) {

        this.interviewId = interviewId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.companyId = companyId;
        this.companyName = companyName;
        this.status = status;
        this.reason = reason;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}