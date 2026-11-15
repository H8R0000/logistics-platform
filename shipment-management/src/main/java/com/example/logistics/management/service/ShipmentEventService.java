package com.example.logistics.management.service;

import com.example.logistics.management.entity.ShipmentEntity;
import com.example.logistics.management.entity.ShipmentEventEntity;
import com.example.logistics.management.entity.ShipmentStatus;
import com.example.logistics.management.kafka.InvalidShipmentEventException;
import com.example.logistics.management.kafka.ShipmentProcessingEvent;
import com.example.logistics.management.repository.ShipmentEventRepository;
import com.example.logistics.management.repository.ShipmentRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A применяет результат B атомарно: основной статус заявки и запись её истории. */
@Service
public class ShipmentEventService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;

    public ShipmentEventService(ShipmentRepository shipmentRepository, ShipmentEventRepository shipmentEventRepository) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
    }

    /** Существующая история хранит eventId; отдельная таблица обработанных сообщений не требуется. */
    @Transactional
    public void apply(ShipmentProcessingEvent event) {
        ShipmentEntity shipment = shipmentRepository.findForUpdate(event.shipmentId())
                .orElseThrow(() -> new InvalidShipmentEventException("Shipment from event does not exist"));
        if (event.sequenceNumber() <= shipment.getLastProcessingSequence()
                || shipmentEventRepository.existsById(event.eventId())) {
            log.info("Ignored duplicate or stale shipment event {} for shipment {}",
                    event.eventId(), event.shipmentId());
            return;
        }
        ShipmentStatus nextStatus = event.payload().status();
        if (!isTransitionAllowed(shipment.getStatus(), nextStatus)) {
            throw new InvalidShipmentEventException("Shipment status cannot change from "
                    + shipment.getStatus() + " to " + nextStatus);
        }
        shipment.applyProcessingResult(nextStatus, event.sequenceNumber(), event.payload().deliveredAt(), Instant.now());
        shipmentEventRepository.save(new ShipmentEventEntity(event.eventId(), event.shipmentId(), event.eventType(),
                nextStatus, event.payload().details(), event.timestamp(), event.sequenceNumber()));
    }

    /** Промежуточное событие могло попасть в DLQ; новый снимок может сразу продвинуть заявку дальше. */
    private boolean isTransitionAllowed(ShipmentStatus current, ShipmentStatus next) {
        return switch (current) {
            case CREATED, PROCESSING, WAITING_FOR_COURIER, FAILED, COURIER_NOT_FOUND -> next != ShipmentStatus.CREATED;
            case IN_TRANSIT -> next == ShipmentStatus.IN_TRANSIT || next == ShipmentStatus.DELIVERED
                    || next == ShipmentStatus.FAILED;
            case DELIVERED -> next == ShipmentStatus.DELIVERED;
        };
    }
}
