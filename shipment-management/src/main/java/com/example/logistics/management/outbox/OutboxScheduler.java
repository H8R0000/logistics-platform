package com.example.logistics.management.outbox;

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

    private final OutboxMessageRepository messages;
    private final OutboxPublisher publisher;
    private final OutboxProperties properties;

    public OutboxScheduler(OutboxMessageRepository messages, OutboxPublisher publisher, OutboxProperties properties) {
        this.messages = messages;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-interval}")
    public void publishPending() {
        for (UUID id : messages.findPendingIds(PageRequest.of(0, properties.batchSize()))) {
            try {
                publisher.publish(id);
            } catch (RuntimeException exception) {
                log.warn("Outbox message {} remains pending and will be retried", id, exception);
                break;
            }
        }
    }
}
