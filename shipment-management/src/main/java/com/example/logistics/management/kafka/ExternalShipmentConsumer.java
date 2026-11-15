package com.example.logistics.management.kafka;

import com.example.logistics.management.service.ShipmentRegistrationService;
import jakarta.validation.Validator;
import io.github.resilience4j.retry.Retry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ExternalShipmentConsumer {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Retry kafkaMessageRetry;
    private final ShipmentRegistrationService shipmentRegistrationService;

    public ExternalShipmentConsumer(ObjectMapper objectMapper, Validator validator, Retry kafkaMessageRetry,
            ShipmentRegistrationService shipmentRegistrationService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.kafkaMessageRetry = kafkaMessageRetry;
        this.shipmentRegistrationService = shipmentRegistrationService;
    }

    /** После commit регистрации контейнер фиксирует offset; повтор безопасен по sourceId/messageId. */
    @KafkaListener(topics = "shipment-requests")
    public void receive(ConsumerRecord<String, String> record) {
        ExternalShipmentRequest request;
        try {
            request = objectMapper.readValue(record.value(), ExternalShipmentRequest.class);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new InvalidExternalShipmentRequestException("Invalid external shipment JSON", exception);
        }
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new InvalidExternalShipmentRequestException("External shipment violates the contract");
        }
        if (!(request.sourceId() + ":" + request.messageId()).equals(record.key())) {
            throw new InvalidExternalShipmentRequestException("Kafka key must be sourceId:messageId");
        }
        kafkaMessageRetry.executeRunnable(() -> shipmentRegistrationService.registerExternal(request));
    }
}
