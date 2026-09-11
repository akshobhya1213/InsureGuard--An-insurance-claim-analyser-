package com.insureguard.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.report-submitted}")
    private String reportSubmittedTopic;

    @Bean
    public NewTopic reportSubmittedTopic() {
        return TopicBuilder.name(reportSubmittedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
