package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.entity.Interview;
import placement_scheduler.repository.InterviewRepository;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin
public class InterviewController {

    private final InterviewRepository interviewRepository;

    public InterviewController(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    @GetMapping
    public ResponseEntity<List<Interview>> getAllInterviews() {

        return ResponseEntity.ok(
                interviewRepository.findAll()
        );
    }

    @GetMapping("/day/{day}")
    public ResponseEntity<List<Interview>> getInterviewsByDay(
            @PathVariable Integer day) {

        return ResponseEntity.ok(
                interviewRepository.findScheduledInterviewsByDay(day)
        );
    }
}
