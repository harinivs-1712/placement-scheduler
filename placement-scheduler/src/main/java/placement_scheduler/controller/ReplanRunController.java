package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.entity.ReplanRun;
import placement_scheduler.entity.InterviewChange;
import placement_scheduler.repository.ReplanRunRepository;
import placement_scheduler.repository.InterviewChangeRepository;
import placement_scheduler.controller.ReplanDetailsResponse;
import placement_scheduler.service.ReplanDetailsService;

import java.util.List;

@RestController
@RequestMapping("/api/replan-runs")
@CrossOrigin
public class ReplanRunController {

    private final ReplanRunRepository replanRunRepository;
    private final ReplanDetailsService replanDetailsService;

    public ReplanRunController(
            ReplanRunRepository replanRunRepository,
            ReplanDetailsService replanDetailsService) {

        this.replanRunRepository = replanRunRepository;
        this.replanDetailsService = replanDetailsService;
    }

    @GetMapping
    public ResponseEntity<List<ReplanRun>> getAllReplanRuns() {
        return ResponseEntity.ok(replanRunRepository.findAll());
    }

    @GetMapping("/{replanId}/details")
    public ResponseEntity<ReplanDetailsResponse> getReplanRunDetails(
            @PathVariable Long replanId) {

        return ResponseEntity.ok(
                replanDetailsService.getReplanDetails(replanId));
    }
}