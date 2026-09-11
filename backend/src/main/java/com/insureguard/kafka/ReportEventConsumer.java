package com.insureguard.kafka;

import com.insureguard.kafka.event.ReportSubmittedEvent;
import com.insureguard.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens for REPORT_SUBMITTED events and kicks off asynchronous
 * text + image analysis for the report.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEventConsumer {

    private final AnalysisService analysisService;

    @KafkaListener(topics = "${app.kafka.topic.report-submitted}", groupId = "${spring.kafka.consumer.group-id}")
    public void onReportSubmitted(ReportSubmittedEvent event) {
        log.info("Consumed REPORT_SUBMITTED event for report {}", event.getReportId());
        try {
            analysisService.processReportAsync(event.getReportId());
        } catch (Exception ex) {
            log.error("Async processing failed for report {}: {}", event.getReportId(), ex.getMessage());
        }
    }
}
