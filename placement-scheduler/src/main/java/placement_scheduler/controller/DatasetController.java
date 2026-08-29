package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.service.DatasetGeneratorService;

@RestController
@RequestMapping("/api/dataset")
public class DatasetController {

    private final DatasetGeneratorService datasetGeneratorService;

    public DatasetController(
            DatasetGeneratorService datasetGeneratorService) {

        this.datasetGeneratorService = datasetGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateDataset(
            @RequestBody DatasetRequest request) {

        datasetGeneratorService.generateDataset(
                request.getStudents(),
                request.getCompanies(),
                request.getRooms(),
                request.getDays()
        );

        return ResponseEntity.ok(
                "Dataset generated successfully."
        );
    }
}

