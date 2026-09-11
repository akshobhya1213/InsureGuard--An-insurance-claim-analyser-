package com.insureguard.service;

import com.insureguard.dto.ReportRequest;
import com.insureguard.dto.ReportResponse;
import com.insureguard.entity.Report;
import com.insureguard.entity.ReportStatus;
import com.insureguard.entity.User;
import com.insureguard.exception.ResourceNotFoundException;
import com.insureguard.kafka.ReportEventProducer;
import com.insureguard.repository.ReportRepository;
import com.insureguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportEventProducer reportEventProducer;

    private static final String UPLOAD_DIR = "./uploads";

    @Transactional
    public ReportResponse createReport(Long userId, ReportRequest request, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = storeImage(image);
        }

        Report report = Report.builder()
                .user(user)
                .description(request.getDescription())
                .imagePath(imagePath)
                .status(ReportStatus.SUBMITTED)
                .build();

        Report saved = reportRepository.save(report);
        log.info("Report {} created by user {}", saved.getId(), userId);

        // Publish asynchronously so text/image analysis doesn't block the API response.
        reportEventProducer.publishReportSubmitted(saved.getId(), userId);

        return toResponse(saved);
    }

    public ReportResponse getReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
        return toResponse(report);
    }

    public Report getReportEntity(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
    }

    public List<ReportResponse> getReportsByUser(Long userId) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void updateStatus(Long reportId, ReportStatus status) {
        Report report = getReportEntity(reportId);
        report.setStatus(status);
        reportRepository.save(report);
    }

    private String storeImage(MultipartFile image) {
        try {
            Path uploadPath = Path.of(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String extension = getExtension(image.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path target = uploadPath.resolve(filename);
            Files.copy(image.getInputStream(), target);
            return target.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store uploaded image: " + ex.getMessage(), ex);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .userId(report.getUser().getId())
                .description(report.getDescription())
                .imagePath(report.getImagePath())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
