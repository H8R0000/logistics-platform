package com.example.logistics.processing.repository;

import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.security.CurrentUser;
import com.example.logistics.processing.service.TaskSearchFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProcessingTaskSpecifications {

    private ProcessingTaskSpecifications() {
    }

    public static Specification<ProcessingTaskEntity> matching(TaskSearchFilter filter, CurrentUser currentUser) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!currentUser.admin()) {
                predicates.add(builder.equal(root.get("ownerId"), currentUser.id()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            if (filter.currentStage() != null) {
                predicates.add(builder.equal(root.get("currentStage"), filter.currentStage()));
            }
            if (filter.shipmentId() != null) {
                predicates.add(builder.equal(root.get("shipmentId"), filter.shipmentId()));
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
