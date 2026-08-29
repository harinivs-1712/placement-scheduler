package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.entity.DisruptionEvent;
import placement_scheduler.repository.DisruptionEventRepository;
import placement_scheduler.controller.StudentWithdrawalRequest;

import java.util.List;

@RestController
@RequestMapping("/api/disruption-events")
@CrossOrigin
public class DisruptionEventController {

    private final DisruptionEventRepository disruptionEventRepository;

    public DisruptionEventController(
            DisruptionEventRepository disruptionEventRepository) {

        this.disruptionEventRepository = disruptionEventRepository;
    }

    @GetMapping
    public ResponseEntity<List<DisruptionEvent>> getAllDisruptionEvents() {

        return ResponseEntity.ok(
                disruptionEventRepository.findAll()
        );
    }
}
