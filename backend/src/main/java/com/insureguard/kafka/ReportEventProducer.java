package com.insureguard.kafka;

import com.insureguard.kafka.event.ReportSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes REPORT_SUBMITTED events so analysis can happen asynchronously. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.report-submitted}")
    private String topic;

    public void publishReportSubmitted(Long reportId, Long userId) {
        ReportSubmittedEvent event = ReportSubmittedEvent.builder()
                .reportId(reportId)
                .userId(userId)
                .eventType("REPORT_SUBMITTED")
                .build();

        kafkaTemplate.send(topic, reportId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish REPORT_SUBMITTED event for report {}: {}", reportId, ex.getMessage());
                    } else {
                        log.info("Published REPORT_SUBMITTED event for report {}", reportId);
                    }
                });
    }
}
