package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.entity.UnscheduledReason;
import placement_scheduler.repository.UnscheduledReasonRepository;

import java.util.List;

@RestController
@RequestMapping("/api/unscheduled-reasons")
@CrossOrigin
public class UnscheduledReasonController {

    private final UnscheduledReasonRepository repository;

    public UnscheduledReasonController(
            UnscheduledReasonRepository repository) {

        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<UnscheduledReason>> getAllReasons() {

        return ResponseEntity.ok(
                repository.findAll()
        );
    }
}
