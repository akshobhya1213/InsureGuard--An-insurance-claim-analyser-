package com.insureguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResultResponse {
    private Long reportId;
    private String overallStatus;
    private Integer fraudScore;
    private TextAnalysisResponse textAnalysis;
    private List<DamageDetectionResponse> damageDetections;
    private String annotatedImagePath;
}
