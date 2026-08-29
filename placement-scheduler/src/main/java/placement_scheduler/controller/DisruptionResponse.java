package placement_scheduler.controller;

import java.util.List;

public class DisruptionResponse {

    private Long replanId;

    private List<Long> affectedInterviewIds;

    private List<Long> affectedStudentIds;

    private int replannedCount;

    private int failedCount;

    public DisruptionResponse() {
    }

    public DisruptionResponse(
            Long replanId,
            List<Long> affectedInterviewIds,
            List<Long> affectedStudentIds,
            int replannedCount,
            int failedCount) {

        this.replanId = replanId;
        this.affectedInterviewIds = affectedInterviewIds;
        this.affectedStudentIds = affectedStudentIds;
        this.replannedCount = replannedCount;
        this.failedCount = failedCount;
    }

    public Long getReplanId() {
        return replanId;
    }

    public void setReplanId(Long replanId) {
        this.replanId = replanId;
    }

    public List<Long> getAffectedInterviewIds() {
        return affectedInterviewIds;
    }

    public void setAffectedInterviewIds(List<Long> affectedInterviewIds) {
        this.affectedInterviewIds = affectedInterviewIds;
    }

    public List<Long> getAffectedStudentIds() {
        return affectedStudentIds;
    }

    public void setAffectedStudentIds(List<Long> affectedStudentIds) {
        this.affectedStudentIds = affectedStudentIds;
    }

    public int getReplannedCount() {
        return replannedCount;
    }

    public void setReplannedCount(int replannedCount) {
        this.replannedCount = replannedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
}