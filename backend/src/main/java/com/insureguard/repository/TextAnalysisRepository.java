package com.insureguard.repository;

import com.insureguard.entity.TextAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TextAnalysisRepository extends JpaRepository<TextAnalysis, Long> {
    Optional<TextAnalysis> findByReportId(Long reportId);
}
