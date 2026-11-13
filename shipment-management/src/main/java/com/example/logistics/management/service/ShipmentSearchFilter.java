package com.example.logistics.management.service;

import com.example.logistics.management.entity.DeliveryType;
import com.example.logistics.management.entity.ShipmentStatus;
import com.example.logistics.management.exception.InvalidQueryException;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record ShipmentSearchFilter(ShipmentStatus status, DeliveryType deliveryType, Instant createdFrom,
                                   Instant createdTo, int page, int size, String sort) {
    private static final Set<String> SORT_FIELDS = Set.of("id", "createdAt", "updatedAt", "status", "deliveryType", "weight");

    public ShipmentSearchFilter {
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new InvalidQueryException("Requested page offset is too large");
        }
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new InvalidQueryException("createdFrom must not be later than createdTo");
        }
    }

    public Pageable pageable() {
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0]) || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new InvalidQueryException("sort must be <field>,<asc|desc>; fields: id, createdAt, updatedAt, status, deliveryType, weight");
        }
        Sort ordering = Sort.by(Sort.Direction.valueOf(parts[1].toUpperCase(Locale.ROOT)), parts[0]);
        if (!parts[0].equals("id")) {
            ordering = ordering.and(Sort.by("id"));
        }
        return PageRequest.of(page, size, ordering);
    }
}
