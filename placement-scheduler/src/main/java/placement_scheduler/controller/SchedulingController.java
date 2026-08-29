package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.service.SchedulingService;

@RestController
@RequestMapping("/api/schedule")
public class SchedulingController {

    private final SchedulingService schedulingService;

    public SchedulingController(
            SchedulingService schedulingService) {

        this.schedulingService = schedulingService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateSchedule() {

        schedulingService.generateSchedule();

        return ResponseEntity.ok(
                "Schedule generated successfully."
        );
    }
}