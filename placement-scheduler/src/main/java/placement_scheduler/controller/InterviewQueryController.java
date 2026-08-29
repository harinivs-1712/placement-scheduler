package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.dto.ScheduledInterviewDTO;
import placement_scheduler.dto.UnscheduledInterviewDTO;
import placement_scheduler.service.InterviewQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin
public class InterviewQueryController {

    private final InterviewQueryService interviewQueryService;

    public InterviewQueryController(
            InterviewQueryService interviewQueryService) {

        this.interviewQueryService = interviewQueryService;
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<ScheduledInterviewDTO>>
    getScheduledInterviews() {

        return ResponseEntity.ok(
                interviewQueryService.getAllScheduled()
        );
    }

    @GetMapping("/unscheduled")
    public ResponseEntity<List<UnscheduledInterviewDTO>>
    getUnscheduledInterviews() {

        return ResponseEntity.ok(
                interviewQueryService.getAllUnscheduled()
        );
    }
}
