package com.example.logistics.processing.workflow;

import com.example.logistics.processing.client.Courier;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class CourierSelector {

    /**
     * Отбирает доступных курьеров с достаточной свободной грузоподъёмностью без активного назначения B.
     * Сортирует по свободной грузоподъёмности, затем по UUID. Фактическое назначение защищается в БД.
     */
    public List<Courier> suitable(List<Courier> couriers, BigDecimal weight, Set<UUID> assigned) {
        return couriers.stream()
            .filter(Courier::available)
            .filter(courier -> !assigned.contains(courier.id()))
            .filter(courier -> courier.remainingCapacity().compareTo(weight) >= 0)
            .sorted(Comparator.comparing(Courier::remainingCapacity)
                .thenComparing(courier -> courier.id().toString()))
            .toList();
    }
}
