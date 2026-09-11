package com.insureguard.controller;

import com.insureguard.dto.ReportResponse;
import com.insureguard.repository.AnalysisResultRepository;
import com.insureguard.repository.UserRepository;
import com.insureguard.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisResultRepository;

    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponse>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/users")
    public ResponseEntity<Long> getUserCount() {
        return ResponseEntity.ok(userRepository.count());
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        long totalReports = reportService.getAllReports().size();
        long suspicious = analysisResultRepository.countByOverallStatus("SUSPICIOUS");
        long reviewRequired = analysisResultRepository.countByOverallStatus("REVIEW REQUIRED");
        long lowRisk = analysisResultRepository.countByOverallStatus("LOW RISK");

        return ResponseEntity.ok(Map.of(
                "totalReports", totalReports,
                "suspicious", suspicious,
                "reviewRequired", reviewRequired,
                "lowRisk", lowRisk
        ));
    }
}
