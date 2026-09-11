package com.insureguard.repository;

import com.insureguard.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByUserId(Long userId);
    List<Report> findByUserIdOrderByCreatedAtDesc(Long userId);
}
