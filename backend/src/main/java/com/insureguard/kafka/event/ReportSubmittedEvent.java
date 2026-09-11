package com.insureguard.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Published to the REPORT_SUBMITTED topic whenever a new claim report is created. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSubmittedEvent implements Serializable {
    private Long reportId;
    private Long userId;
    private String eventType;
}
