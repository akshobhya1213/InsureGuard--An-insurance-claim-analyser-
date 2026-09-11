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
public class TextAnalysisResponse {
    private List<String> keywords;
    private List<String> entities;
    private List<String> concepts;
    private Object sentiment;
    private Integer suspicionScore;
    private String status;
    private List<String> indicators;
}
