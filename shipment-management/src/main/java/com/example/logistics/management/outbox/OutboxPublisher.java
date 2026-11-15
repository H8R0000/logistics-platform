package com.example.logistics.management.outbox;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisher {

    private final OutboxMessageRepository messages;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxMessageRepository messages, KafkaTemplate<String, String> kafka) {
        this.messages = messages;
        this.kafka = kafka;
    }

    @Transactional
    public void publish(UUID id) {
        OutboxMessageEntity message = messages.findForPublication(id).orElseThrow();
        if (message.getPublishedAt() != null) {
            return;
        }
        try {
            kafka.send("shipment-processing", message.getShipmentId().toString(), message.getPayload()).get(15, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Outbox publication interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Kafka did not acknowledge the outbox message", exception);
        }
        message.markPublished(Instant.now());
    }
}
