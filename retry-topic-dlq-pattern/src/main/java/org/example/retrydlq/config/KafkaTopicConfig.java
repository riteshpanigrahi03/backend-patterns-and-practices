package org.example.retrydlq.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private final String mainTopic;
    private final String retryTopic;
    private final String dlqTopic;

    public KafkaTopicConfig(
            @Value("${app.topics.main}") String mainTopic,
            @Value("${app.topics.retry}") String retryTopic,
            @Value("${app.topics.dlq}") String dlqTopic
    ) {
        this.mainTopic = mainTopic;
        this.retryTopic = retryTopic;
        this.dlqTopic = dlqTopic;
    }

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(mainTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderEventsRetryTopic() {
        return TopicBuilder.name(retryTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderEventsDlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
