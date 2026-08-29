package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.dto.StudentOverviewDTO;
import placement_scheduler.service.StudentOverviewService;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin
public class StudentController {

    private final StudentOverviewService studentOverviewService;

    public StudentController(
            StudentOverviewService studentOverviewService) {

        this.studentOverviewService = studentOverviewService;
    }

    @GetMapping("/overview")
    public ResponseEntity<List<StudentOverviewDTO>> getStudentOverview() {

        return ResponseEntity.ok(
                studentOverviewService.getStudentOverview()
        );
    }
}