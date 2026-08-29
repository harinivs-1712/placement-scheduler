package placement_scheduler.service;

import java.time.LocalTime;

public class PreviousAssignment {

    private final Integer day;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Long panelId;
    private final Long roomId;

    public PreviousAssignment(
            Integer day,
            LocalTime startTime,
            LocalTime endTime,
            Long panelId,
            Long roomId) {

        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.panelId = panelId;
        this.roomId = roomId;
    }

    public Integer getDay() {
        return day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Long getPanelId() {
        return panelId;
    }

    public Long getRoomId() {
        return roomId;
    }
}