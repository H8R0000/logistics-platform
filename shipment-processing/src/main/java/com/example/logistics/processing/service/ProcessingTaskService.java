package com.example.logistics.processing.service;

import com.example.logistics.processing.dto.PageResponse;
import com.example.logistics.processing.dto.TaskEventResponse;
import com.example.logistics.processing.dto.TaskResponse;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.exception.TaskNotFoundException;
import com.example.logistics.processing.mapper.ProcessingTaskMapper;
import com.example.logistics.processing.repository.ProcessingTaskEventRepository;
import com.example.logistics.processing.repository.ProcessingTaskRepository;
import com.example.logistics.processing.repository.ProcessingTaskSpecifications;
import com.example.logistics.processing.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class ProcessingTaskService {

    private final ProcessingTaskRepository tasks;
    private final ProcessingTaskEventRepository events;
    private final ProcessingTaskMapper mapper;

    public ProcessingTaskService(ProcessingTaskRepository tasks, ProcessingTaskEventRepository events,
                                 ProcessingTaskMapper mapper) {
        this.tasks = tasks;
        this.events = events;
        this.mapper = mapper;
    }

    public PageResponse<TaskResponse> list(TaskSearchFilter filter, CurrentUser currentUser) {
        return PageResponse.from(tasks.findAll(ProcessingTaskSpecifications.matching(filter, currentUser),
                filter.pageable()).map(mapper::toResponse));
    }

    public TaskResponse get(UUID id, CurrentUser currentUser) {
        return mapper.toResponse(findAccessible(id, currentUser));
    }

    public List<TaskEventResponse> history(UUID id, CurrentUser currentUser) {
        findAccessible(id, currentUser);
        return events.findByTaskIdOrderByCreatedAtAscIdAsc(id).stream()
                .map(mapper::toEventResponse).toList();
    }

    private ProcessingTaskEntity findAccessible(UUID id, CurrentUser currentUser) {
        return (currentUser.admin() ? tasks.findById(id) : tasks.findByIdAndOwnerId(id, currentUser.id()))
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
