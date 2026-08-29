package placement_scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import placement_scheduler.entity.Panel;
import placement_scheduler.repository.PanelRepository;

import java.util.List;

@RestController
@RequestMapping("/api/panels")
@CrossOrigin
public class PanelController {

    private final PanelRepository panelRepository;

    public PanelController(PanelRepository panelRepository) {
        this.panelRepository = panelRepository;
    }

    @GetMapping
    public ResponseEntity<List<Panel>> getAllPanels() {

        return ResponseEntity.ok(
                panelRepository.findAll()
        );
    }
}