package com.example.logistics.processing.repository;

import com.example.logistics.processing.entity.ProcessingTaskEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingTaskEventRepository extends JpaRepository<ProcessingTaskEventEntity, UUID> {

    List<ProcessingTaskEventEntity> findByTaskIdOrderByCreatedAtAscIdAsc(UUID taskId);
}
