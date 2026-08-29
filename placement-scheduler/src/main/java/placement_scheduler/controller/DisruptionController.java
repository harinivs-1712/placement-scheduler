package placement_scheduler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.service.DisruptionService;
import placement_scheduler.service.ReplanResult;
import placement_scheduler.controller.StudentWithdrawalRequest;

/**
 * Exposes disruption injection as REST endpoints. Only PANEL_DROP exists
 * for now — ROOM_UNAVAILABLE, STUDENT_WITHDRAWAL, and COMPANY_DELAY will
 * get their own endpoints here once their service methods are built,
 * following the same shape.
 */
@RestController
@RequestMapping("/api/disruptions")
@CrossOrigin
public class DisruptionController {

    private final DisruptionService disruptionService;

    public DisruptionController(DisruptionService disruptionService) {
        this.disruptionService = disruptionService;
    }

    /**
     * Example request body:
     * {
     * "panelId": 42,
     * "day": 1,
     * "effectiveTime": "14:00",
     * "details": "Interviewer called in sick"
     * }
     *
     * Interviews on this panel, on this day, starting at or after
     * effectiveTime are invalidated and replanned. Earlier interviews
     * that day are untouched. The panel itself is deactivated for good,
     * on every day, once dropped.
     *
     * "details" is optional.
     */
    @PostMapping("/panel-drop")
    public ResponseEntity<?> dropPanel(@RequestBody PanelDropRequest request) {

        if (request.getPanelId() == null || request.getDay() == null || request.getEffectiveTime() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("panelId, day, and effectiveTime are all required.");
        }

        try {
            DisruptionResponse result = disruptionService.handlePanelDrop(request);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException ex) {
            // No panel found with that id
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());

        } catch (IllegalStateException ex) {
            // Panel already INACTIVE — already dropped
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    @PostMapping("/room-unavailable")
    public ResponseEntity<?> roomUnavailable(
            @RequestBody RoomUnavailableRequest request) {

        if (request.getRoomId() == null
                || request.getDay() == null
                || request.getEffectiveTime() == null) {

            return ResponseEntity
                    .badRequest()
                    .body("roomId, day, and effectiveTime are all required.");
        }

        try {
            DisruptionResponse result = disruptionService.handleRoomUnavailable(request);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());

        } catch (IllegalStateException ex) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    @PostMapping("/student-withdrawal")
    public ResponseEntity<?> withdrawStudent(@RequestBody StudentWithdrawalRequest request) {

        if (request.getStudentId() == null || request.getDay() == null || request.getEffectiveTime() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("studentId, day, and effectiveTime are all required.");
        }

        try {
            ReplanResult result = disruptionService.handleStudentWithdrawal(request);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());

        } catch (IllegalStateException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }
}