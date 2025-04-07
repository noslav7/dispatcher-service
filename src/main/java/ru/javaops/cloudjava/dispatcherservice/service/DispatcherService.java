package ru.javaops.cloudjava.dispatcherservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import ru.javaops.cloudjava.AvroOrderPlacedEvent;
import ru.javaops.cloudjava.OrderDispatchStatus;
import ru.javaops.cloudjava.OrderDispatchedEvent;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class DispatcherService {

    private final KafkaTemplate<String, OrderDispatchedEvent> kafkaTemplate;
    @Value("${kafkaprops.order-dispatch-topic}")
    private String orderDispatchTopic;
    @Value("${kafkaprops.nack-sleep-duration}")
    private Duration nackSleepDuration;

    @KafkaListener(topics = {"${kafkaprops.order-placed-topic}"})
    public void consumeOrderPlacedEvent(AvroOrderPlacedEvent event,
                                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        Acknowledgment acknowledgment) {
        log.info("Consuming message from Kafka: {}. Key: {}. Partition: {}. Topic: {}",
                event, key, partition, topic);
        var orderDispatchEvent = processEvent(event);
        try {
            kafkaTemplate.send(orderDispatchTopic, key, orderDispatchEvent).get();
            log.info("Successfully processed and sent OrderDispatchedEvent {} to Kafka.",
                    orderDispatchEvent);
            // commit the offset
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send OrderDispatchedEvent {} to Kafka", orderDispatchEvent);
            // don't commit the offset
            acknowledgment.nack(nackSleepDuration);
        }
    }

    private OrderDispatchedEvent processEvent(AvroOrderPlacedEvent event) {
        log.info("Processing OrderPlacedEvent: {}", event);
        OrderDispatchStatus status = event.getOrderId() % 2 == 0 ? OrderDispatchStatus.ACCEPTED : OrderDispatchStatus.REJECTED;
        return OrderDispatchedEvent.newBuilder()
                .setOrderId(event.getOrderId())
                .setStatus(status)
                .build();
    }
}