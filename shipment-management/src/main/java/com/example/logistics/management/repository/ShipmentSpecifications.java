package com.example.logistics.management.repository;

import com.example.logistics.management.entity.ShipmentEntity;
import com.example.logistics.management.security.CurrentUser;
import com.example.logistics.management.service.ShipmentSearchFilter;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public final class ShipmentSpecifications {

    private ShipmentSpecifications() {
    }

    public static Specification<ShipmentEntity> matching(ShipmentSearchFilter filter, CurrentUser currentUser) {

        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!currentUser.admin()) {
                predicates.add(builder.equal(root.get("ownerId"), currentUser.id()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            if (filter.deliveryType() != null) {
                predicates.add(builder.equal(root.get("deliveryType"), filter.deliveryType()));
            }
            if (filter.createdFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
            }
            if (filter.createdTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
