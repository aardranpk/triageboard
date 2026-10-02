package com.aardranpk.triageboard.triage;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/triage")
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    @GetMapping("/queue")
    public List<TriageEntry> queue() {
        return triageService.queue();
    }

    @GetMapping("/next")
    public ResponseEntity<TriageEntry> next() {
        return triageService.next()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/workload")
    public Map<Long, Integer> workload() {
        return triageService.workload();
    }
}