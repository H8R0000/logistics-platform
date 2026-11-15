package com.example.logistics.processing.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.logistics.processing.client.Courier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourierSelectorTest {

    private final CourierSelector selector = new CourierSelector();

    @Test
    void excludesUnavailableAssignedAndOverloadedCouriers() {
        Courier unavailable = courier("100", "0", false);
        Courier assigned = courier("100", "0", true);
        Courier overloaded = courier("20", "15.001", true);
        Courier exactCapacity = courier("20", "15", true);
        Courier largerCapacity = courier("50", "0", true);

        List<Courier> result = selector.suitable(
                List.of(unavailable, assigned, overloaded, largerCapacity, exactCapacity),
                new BigDecimal("5"), Set.of(assigned.id()));

        assertThat(result).containsExactly(exactCapacity, largerCapacity);
    }

    @Test
    void returnsEmptyListWhenNoCourierCanCarryShipment() {
        assertThat(selector.suitable(List.of(courier("10", "9", true)),
                new BigDecimal("2"), Set.of())).isEmpty();
    }

    private Courier courier(String maxWeight, String currentLoad, boolean available) {
        return new Courier(UUID.randomUUID(), "Courier", Courier.VehicleType.CAR,
                new BigDecimal(maxWeight), new BigDecimal(currentLoad), available, Instant.EPOCH);
    }
}
