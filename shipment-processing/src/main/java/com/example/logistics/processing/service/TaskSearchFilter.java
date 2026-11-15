package com.example.logistics.processing.service;

import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.exception.InvalidQueryException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@NullMarked
public record TaskSearchFilter(@Nullable ProcessingTaskStatus status, @Nullable ProcessingStage currentStage,
                               @Nullable UUID shipmentId, @Nullable Instant createdFrom,
                               @Nullable Instant createdTo, int page, int size, String sort) {

    private static final Set<String> SORT_FIELDS = Set.of("id", "shipmentId", "status", "currentStage",
            "attempts", "createdAt", "startedAt", "nextExecutionAt", "completedAt");

    public TaskSearchFilter {
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new InvalidQueryException("Requested page offset is too large");
        }
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new InvalidQueryException("createdFrom must not be later than createdTo");
        }
    }

    public Pageable pageable() {
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new InvalidQueryException("sort must be <field>,<asc|desc>; fields: id, shipmentId, status, "
                    + "currentStage, attempts, createdAt, startedAt, nextExecutionAt, completedAt");
        }
        Sort ordering = Sort.by(Sort.Direction.valueOf(parts[1].toUpperCase(Locale.ROOT)), parts[0]);
        if (!parts[0].equals("id")) {
            ordering = ordering.and(Sort.by("id"));
        }
        return PageRequest.of(page, size, ordering);
    }
}
