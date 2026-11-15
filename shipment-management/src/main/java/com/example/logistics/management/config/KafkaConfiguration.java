package com.example.logistics.management.config;

import com.example.logistics.management.kafka.InvalidExternalShipmentRequestException;
import com.example.logistics.management.kafka.InvalidShipmentEventException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfiguration.class);

    /** Короткие повторы вызова транзакционного сервиса; каждая попытка открывает новую транзакцию. */
    @Bean
    Retry kafkaMessageRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
                .retryOnException(exception -> exception instanceof TransientDataAccessException
                        || exception instanceof DataAccessResourceFailureException
                        || exception instanceof RecoverableDataAccessException
                        || exception instanceof CannotCreateTransactionException
                        || exception instanceof TransactionSystemException)
                .build();
        Retry retry = Retry.of("kafka-message", config);
        retry.getEventPublisher().onRetry(event -> {
            Throwable failure = event.getLastThrowable();
            log.warn("Kafka processing retry {}; cause {}", event.getNumberOfRetryAttempts(),
                    failure == null ? "retryable result" : failure.getClass().getSimpleName());
        });
        return retry;
    }

    @Bean
    DefaultErrorHandler shipmentConsumerErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition("shipment-dlq", record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(15));
        // Повторы уже выполнены Resilience4j; здесь только восстановление через DLQ.
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(0, 0));
        handler.addNotRetryableExceptions(InvalidShipmentEventException.class, InvalidExternalShipmentRequestException.class);
        return handler;
    }
}
