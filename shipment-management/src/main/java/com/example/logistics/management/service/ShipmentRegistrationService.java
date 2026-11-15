package com.example.logistics.management.service;

import com.example.logistics.management.config.ExternalShipmentProperties;
import com.example.logistics.management.dto.CreateShipmentRequest;
import com.example.logistics.management.dto.ShipmentAcceptedResponse;
import com.example.logistics.management.entity.ShipmentEntity;
import com.example.logistics.management.entity.ShipmentEventEntity;
import com.example.logistics.management.kafka.ExternalShipmentRequest;
import com.example.logistics.management.kafka.InvalidExternalShipmentRequestException;
import com.example.logistics.management.kafka.ShipmentProcessingCommand;
import com.example.logistics.management.mapper.ShipmentMapper;
import com.example.logistics.management.outbox.OutboxMessageEntity;
import com.example.logistics.management.outbox.OutboxMessageRepository;
import com.example.logistics.management.repository.ShipmentEventRepository;
import com.example.logistics.management.repository.ShipmentRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Внутренняя регистрация: каждый вход определяет доверенного владельца до сохранения. */
@Service
@EnableConfigurationProperties(ExternalShipmentProperties.class)
public class ShipmentRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentRegistrationService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final ShipmentMapper shipmentMapper;
    private final ObjectMapper objectMapper;
    private final ExternalShipmentProperties externalShipmentProperties;

    public ShipmentRegistrationService(ShipmentRepository shipmentRepository, ShipmentEventRepository shipmentEventRepository,
            OutboxMessageRepository outboxMessageRepository, ShipmentMapper shipmentMapper,
            ObjectMapper objectMapper, ExternalShipmentProperties externalShipmentProperties) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.shipmentMapper = shipmentMapper;
        this.objectMapper = objectMapper;
        this.externalShipmentProperties = externalShipmentProperties;
    }

    /** ownerId поступает из защищённого REST-сервиса, который извлёк его из проверенного JWT. */
    @Transactional
    public ShipmentAcceptedResponse register(CreateShipmentRequest request, String ownerId) {
        Instant now = Instant.now();
        ShipmentEntity shipment = shipmentRepository.save(shipmentMapper.toEntity(request, UUID.randomUUID(), ownerId, now));
        saveHistoryAndOutbox(shipment, "Shipment registered", now);
        return new ShipmentAcceptedResponse(shipment.getId(), shipment.getStatus());
    }

    /** Вставка заявки, история и outbox фиксируются вместе; конфликт ключа означает уже принятую операцию. */
    @Transactional
    public void registerExternal(ExternalShipmentRequest request) {
        if (!externalShipmentProperties.sourceId().equals(request.sourceId())) {
            throw new InvalidExternalShipmentRequestException("External source is not allowed");
        }
        Instant now = Instant.now();
        ShipmentEntity shipment = shipmentMapper.toEntity(request.shipmentData(), UUID.randomUUID(),
                externalShipmentProperties.ownerId(), now);
        int inserted = shipmentRepository.insertExternalIfAbsent(shipment.getId(), shipment.getOwnerId(),
                request.sourceId(), request.messageId(), objectMapper.writeValueAsString(shipment.getOrigin()),
                objectMapper.writeValueAsString(shipment.getDestination()), shipment.getWeight(),
                objectMapper.writeValueAsString(shipment.getDimensions()), shipment.getDeliveryType().name(), now);
        if (inserted == 0) {
            log.info("Ignored duplicate external shipment from {} with message {}", request.sourceId(), request.messageId());
            return;
        }
        saveHistoryAndOutbox(shipment, "Shipment registered from source " + request.sourceId()
                + "; message " + request.messageId(), now);
    }

    private void saveHistoryAndOutbox(ShipmentEntity shipment, String details, Instant now) {
        shipmentEventRepository.save(new ShipmentEventEntity(UUID.randomUUID(), shipment.getId(),
                "SHIPMENT_CREATED", shipment.getStatus(), details, now));
        UUID messageId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(ShipmentProcessingCommand.from(shipment, messageId));
        outboxMessageRepository.save(new OutboxMessageEntity(messageId, shipment.getId(), payload, now));
        log.info("Registered shipment {} for owner {}", shipment.getId(), shipment.getOwnerId());
    }
}
