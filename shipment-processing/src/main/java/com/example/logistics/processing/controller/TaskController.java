package com.example.logistics.processing.controller;

import com.example.logistics.processing.dto.PageResponse;
import com.example.logistics.processing.dto.TaskEventResponse;
import com.example.logistics.processing.dto.TaskResponse;
import com.example.logistics.processing.dto.TaskStatisticsResponse;
import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.security.CurrentUser;
import com.example.logistics.processing.service.ProcessingTaskService;
import com.example.logistics.processing.service.ProcessingTaskAdminService;
import com.example.logistics.processing.service.TaskSearchFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final ProcessingTaskService service;
    private final ProcessingTaskAdminService processingTaskAdminService;

    public TaskController(ProcessingTaskService service, ProcessingTaskAdminService processingTaskAdminService) {
        this.service = service;
        this.processingTaskAdminService = processingTaskAdminService;
    }

    @GetMapping
    public PageResponse<TaskResponse> list(
            @RequestParam(required = false) ProcessingTaskStatus status,
            @RequestParam(required = false) ProcessingStage currentStage,
            @RequestParam(required = false) UUID shipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            JwtAuthenticationToken authentication) {
        TaskSearchFilter filter = new TaskSearchFilter(status, currentStage, shipmentId,
                createdFrom, createdTo, page, size, sort);
        return service.list(filter, CurrentUser.from(authentication));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable UUID id, JwtAuthenticationToken authentication) {
        return service.get(id, CurrentUser.from(authentication));
    }

    @GetMapping("/{id}/events")
    public List<TaskEventResponse> history(@PathVariable UUID id, JwtAuthenticationToken authentication) {
        return service.history(id, CurrentUser.from(authentication));
    }

    @GetMapping("/statistics")
    public TaskStatisticsResponse statistics() {
        return processingTaskAdminService.statistics();
    }

    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskResponse retry(@PathVariable UUID id, JwtAuthenticationToken authentication) {
        return processingTaskAdminService.retry(id, CurrentUser.from(authentication));
    }
}
