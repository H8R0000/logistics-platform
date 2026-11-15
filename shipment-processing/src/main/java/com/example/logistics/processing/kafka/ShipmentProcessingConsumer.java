package com.example.logistics.processing.kafka;

import com.example.logistics.processing.service.ProcessingTaskRegistrationService;
import jakarta.validation.Validator;
import io.github.resilience4j.retry.Retry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ShipmentProcessingConsumer {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Retry kafkaMessageRetry;
    private final ProcessingTaskRegistrationService registration;

    public ShipmentProcessingConsumer(ObjectMapper objectMapper, Validator validator, Retry kafkaMessageRetry, ProcessingTaskRegistrationService registration) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.kafkaMessageRetry = kafkaMessageRetry;
        this.registration = registration;
    }

    @KafkaListener(topics = "shipment-processing")
    public void receive(ConsumerRecord<String, String> record) {
        ShipmentProcessingCommand command;
        try {
            command = objectMapper.readValue(record.value(), ShipmentProcessingCommand.class);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new InvalidProcessingCommandException("Invalid processing command JSON", exception);
        }
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new InvalidProcessingCommandException("Processing command violates the contract");
        }
        if (!command.shipmentId().toString().equals(record.key())) {
            throw new InvalidProcessingCommandException("Kafka key must match shipmentId");
        }
        kafkaMessageRetry.executeRunnable(() -> registration.register(command));
    }
}
