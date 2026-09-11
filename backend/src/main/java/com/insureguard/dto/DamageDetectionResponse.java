package com.insureguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageDetectionResponse {
    private String damageType;
    private Double confidence;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
}
