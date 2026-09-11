package com.insureguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private Long userId;
    private String description;
    private String imagePath;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
