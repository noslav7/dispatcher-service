package ru.javaops.cloudjava.dispatcherservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafkaprops.order-dispatch-topic}")
    private String ordersDispatchTopic;
    @Value("${kafkaprops.replication-factor}")
    private Integer replicationFactor;
    @Value("${kafkaprops.partitions-count}")
    private Integer partitionsCount;

    @Bean
    public NewTopic ordersDispatchTopic() {
        return TopicBuilder
                .name(ordersDispatchTopic)
                .replicas(replicationFactor)
                .partitions(partitionsCount)
                .build();
    }
}
