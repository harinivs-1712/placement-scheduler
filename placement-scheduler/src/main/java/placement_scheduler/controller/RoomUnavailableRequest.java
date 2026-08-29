package placement_scheduler.controller;

/**
 * Request body for POST /api/disruptions/room-unavailable.
 *
 * Same shape and semantics as PanelDropRequest: interviews in this room,
 * on this day, starting at or after effectiveTime are invalidated and
 * replanned. Earlier interviews that day are untouched. Unlike a panel
 * drop, the affected interviews here can belong to MULTIPLE different
 * companies at once, since a room is shared infrastructure, not owned by
 * one company.
 */
public class RoomUnavailableRequest {

    private Long roomId;
    private Integer day;
    private String effectiveTime;
    private String details;

    public RoomUnavailableRequest() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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
