package com.example.logistics.processing.outbox;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final OutboxPublisher outboxPublisher;
    private final OutboxProperties outboxProperties;

    public OutboxScheduler(OutboxMessageRepository outboxMessageRepository, OutboxPublisher outboxPublisher,
            OutboxProperties outboxProperties) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.outboxPublisher = outboxPublisher;
        this.outboxProperties = outboxProperties;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-interval}")
    public void publishPending() {
        publishPendingTopic("shipment-events");
        publishPendingTopic("shipment-errors");
    }

    private void publishPendingTopic(String topic) {
        for (UUID id : outboxMessageRepository.findPublishableIds(topic, PageRequest.of(0, outboxProperties.batchSize()))) {
            try {
                outboxPublisher.publish(id);
            } catch (RuntimeException exception) {
                log.warn("Outbox event {} remains pending and will be retried", id, exception);
                break;
            }
        }
    }
}
