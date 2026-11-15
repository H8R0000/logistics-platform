package com.example.logistics.management.kafka;

import com.example.logistics.management.service.ShipmentEventService;
import jakarta.validation.Validator;
import io.github.resilience4j.retry.Retry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ShipmentEventConsumer {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Retry kafkaMessageRetry;
    private final ShipmentEventService shipmentEventService;

    public ShipmentEventConsumer(ObjectMapper objectMapper, Validator validator, Retry kafkaMessageRetry, ShipmentEventService shipmentEventService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.kafkaMessageRetry = kafkaMessageRetry;
        this.shipmentEventService = shipmentEventService;
    }

    /** Offset фиксируется контейнером после возврата из метода, когда транзакция A уже завершена. */
    @KafkaListener(topics = "shipment-events")
    public void receive(ConsumerRecord<String, String> record) {
        ShipmentProcessingEvent event;
        try {
            event = objectMapper.readValue(record.value(), ShipmentProcessingEvent.class);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new InvalidShipmentEventException("Invalid shipment event JSON", exception);
        }
        if (event == null || !validator.validate(event).isEmpty() || !event.hasConsistentResult()) {
            throw new InvalidShipmentEventException("Shipment event violates the contract");
        }
        if (!event.shipmentId().toString().equals(record.key())) {
            throw new InvalidShipmentEventException("Kafka key must match shipmentId");
        }
        kafkaMessageRetry.executeRunnable(() -> shipmentEventService.apply(event));
    }
}
