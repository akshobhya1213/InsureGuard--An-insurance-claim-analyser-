package com.insureguard.repository;

import com.insureguard.entity.DamageDetection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DamageDetectionRepository extends JpaRepository<DamageDetection, Long> {
    List<DamageDetection> findByReportId(Long reportId);
}
