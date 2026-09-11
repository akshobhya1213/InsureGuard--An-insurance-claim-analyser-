package com.insureguard.repository;

import com.insureguard.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    Optional<AnalysisResult> findByReportId(Long reportId);
    long countByOverallStatus(String overallStatus);
}
