package com.insureguard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Stores the Watson NLU output plus the derived, explainable fraud/suspicion score. */
@Entity
@Table(name = "text_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @Lob
    private String keywords;

    @Lob
    private String entities;

    @Lob
    private String concepts;

    @Lob
    private String sentiment;

    @Column(name = "suspicion_score")
    private Integer suspicionScore;

    @Lob
    @Column(name = "suspicion_indicators")
    private String suspicionIndicators;

    @Column(length = 30)
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
