package com.example.logistics.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.logistics.management.entity.Address;
import com.example.logistics.management.entity.DeliveryType;
import com.example.logistics.management.entity.Dimensions;
import com.example.logistics.management.entity.ShipmentEntity;
import com.example.logistics.management.entity.ShipmentEventEntity;
import com.example.logistics.management.entity.ShipmentStatus;
import com.example.logistics.management.kafka.InvalidShipmentEventException;
import com.example.logistics.management.kafka.ShipmentProcessingEvent;
import com.example.logistics.management.repository.ShipmentEventRepository;
import com.example.logistics.management.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentEventServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentEventRepository shipmentEventRepository;

    private ShipmentEventService service;
    private ShipmentEntity shipment;

    @BeforeEach
    void setUp() {
        service = new ShipmentEventService(shipmentRepository, shipmentEventRepository);
        Address address = new Address("Vilnius", "Lithuania", "Street 1");
        shipment = new ShipmentEntity(UUID.randomUUID(), "owner", address, address, BigDecimal.ONE,
                new Dimensions(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE), DeliveryType.STANDARD, NOW);
        when(shipmentRepository.findForUpdate(shipment.getId())).thenReturn(Optional.of(shipment));
    }

    @Test
    void appliesDeliveryResultAndKeepsIncomingEventIdInHistory() {
        ShipmentProcessingEvent event = event(ShipmentStatus.DELIVERED, 5);

        service.apply(event);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isEqualTo(NOW);
        assertThat(shipment.getLastProcessingSequence()).isEqualTo(5);
        ArgumentCaptor<ShipmentEventEntity> history = ArgumentCaptor.forClass(ShipmentEventEntity.class);
        verify(shipmentEventRepository).save(history.capture());
        assertThat(history.getValue().getId()).isEqualTo(event.eventId());
        assertThat(history.getValue().getShipmentId()).isEqualTo(shipment.getId());
        assertThat(history.getValue().getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void repeatedEventDoesNotCreateSecondHistoryEntry() {
        ShipmentProcessingEvent event = event(ShipmentStatus.DELIVERED, 5);

        service.apply(event);
        Instant updatedAt = shipment.getUpdatedAt();
        service.apply(event);

        verify(shipmentEventRepository, times(1)).save(any(ShipmentEventEntity.class));
        assertThat(shipment.getLastProcessingSequence()).isEqualTo(5);
        assertThat(shipment.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void staleEventDoesNotRollBackDeliveredShipment() {
        shipment.applyProcessingResult(ShipmentStatus.DELIVERED, 5, NOW, NOW);

        service.apply(event(ShipmentStatus.IN_TRANSIT, 4));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getLastProcessingSequence()).isEqualTo(5);
        assertThat(shipment.getDeliveredAt()).isEqualTo(NOW);
        verifyNoInteractions(shipmentEventRepository);
    }

    @Test
    void evenNewerEventCannotReopenDeliveredShipment() {
        shipment.applyProcessingResult(ShipmentStatus.DELIVERED, 5, NOW, NOW);

        assertThatThrownBy(() -> service.apply(event(ShipmentStatus.PROCESSING, 6)))
                .isInstanceOf(InvalidShipmentEventException.class);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getLastProcessingSequence()).isEqualTo(5);
        verify(shipmentEventRepository, never()).save(any(ShipmentEventEntity.class));
    }

    private ShipmentProcessingEvent event(ShipmentStatus status, long sequence) {
        return new ShipmentProcessingEvent(1, UUID.randomUUID(), shipment.getId(),
                status == ShipmentStatus.PROCESSING ? "SHIPMENT_PROCESSING_STARTED" : "SHIPMENT_" + status,
                NOW, new ShipmentProcessingEvent.Payload(UUID.randomUUID(), status,
                        status == ShipmentStatus.PROCESSING ? "SEARCH_COURIER" : "CONFIRM_DELIVERY",
                        "Processing result", status == ShipmentStatus.DELIVERED ? NOW : null),
                shipment.getId(), sequence);
    }
}
