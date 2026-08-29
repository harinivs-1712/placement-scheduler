package placement_scheduler.controller;

/**
 * Request body for POST /api/disruptions/student-withdrawal.
 *
 * Withdrawal is FULL — the student is cancelled out of every company's
 * process, not just one. day/effectiveTime work the same as panel/room
 * disruptions: SCHEDULED interviews already completed before this point
 * are left untouched; everything at/after it is cancelled. UNSCHEDULED
 * interviews for this student are cancelled unconditionally, since they
 * never held a day/time to compare against.
 */
public class StudentWithdrawalRequest {

    private Long studentId;
    private Integer day;
    private String effectiveTime;
    private String details;

    public StudentWithdrawalRequest() {
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public String getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(String effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}