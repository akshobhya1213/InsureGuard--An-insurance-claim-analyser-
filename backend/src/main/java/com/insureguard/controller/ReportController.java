package com.insureguard.controller;

import com.insureguard.dto.ReportRequest;
import com.insureguard.dto.ReportResponse;
import com.insureguard.security.UserPrincipal;
import com.insureguard.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("report") ReportRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        ReportResponse response = reportService.createReport(principal.getId(), request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReport(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportResponse>> getReportsByUser(@PathVariable Long userId,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        // USERs may only view their own reports; ADMINs can view any user's reports.
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !principal.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reportService.getReportsByUser(userId));
    }
}
