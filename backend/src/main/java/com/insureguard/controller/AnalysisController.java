package com.insureguard.controller;

import com.insureguard.dto.AnalysisResultResponse;
import com.insureguard.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /** Runs both text and image analysis synchronously and returns the combined result. */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<AnalysisResultResponse> analyze(@PathVariable Long id) {
        return ResponseEntity.ok(analysisService.analyzeReport(id));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<AnalysisResultResponse> getResult(@PathVariable Long id) {
        return ResponseEntity.ok(analysisService.getResult(id));
    }
}
