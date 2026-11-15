package com.example.logistics.processing.outbox;

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

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxMessageRepository outboxMessageRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Блокировка держится до подтверждения Kafka; при сбое допускается повтор того же eventId. */
    @Transactional
    public void publish(UUID id) {
        OutboxMessageEntity message = outboxMessageRepository.findForPublication(id).orElseThrow();
        if (message.getPublishedAt() != null) {
            return;
        }
        try {
            kafkaTemplate.send(message.getTopic(), message.getShipmentId().toString(), message.getPayload())
                    .get(15, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Outbox publication interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Kafka did not acknowledge the outbox event", exception);
        }
        message.markPublished(Instant.now());
    }
}
