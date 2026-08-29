package placement_scheduler.dto;

import java.util.List;

public class StudentOverviewDTO {

    private Long studentId;
    private String name;
    private String email;
    private Double cgpa;
    private String status;

    private List<StudentInterviewDTO> interviews;

    public StudentOverviewDTO() {
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<StudentInterviewDTO> getInterviews() {
        return interviews;
    }

    public void setInterviews(List<StudentInterviewDTO> interviews) {
        this.interviews = interviews;
    }
}