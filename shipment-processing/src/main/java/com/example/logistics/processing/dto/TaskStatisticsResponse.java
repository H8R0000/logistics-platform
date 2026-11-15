package com.example.logistics.processing.dto;

import com.example.logistics.processing.entity.ProcessingTaskStatus;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record TaskStatisticsResponse(long total, Map<ProcessingTaskStatus, Long> byStatus) {
}
