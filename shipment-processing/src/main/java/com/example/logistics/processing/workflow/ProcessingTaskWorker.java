package com.example.logistics.processing.workflow;

import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.repository.ProcessingTaskRepository;
import com.example.logistics.processing.repository.TaskExecutionSnapshot;
import com.example.logistics.processing.service.DeliveryWorkflowService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Направляет задачу в поиск курьера либо в транзакционный этап маршрута/доставки. */
@Component
public class ProcessingTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(ProcessingTaskWorker.class);
    private final ProcessingTaskRepository processingTaskRepository;
    private final CourierSearchWorker courierSearchWorker;
    private final DeliveryWorkflowService deliveryWorkflowService;

    public ProcessingTaskWorker(ProcessingTaskRepository processingTaskRepository,
            CourierSearchWorker courierSearchWorker, DeliveryWorkflowService deliveryWorkflowService) {
        this.processingTaskRepository = processingTaskRepository;
        this.courierSearchWorker = courierSearchWorker;
        this.deliveryWorkflowService = deliveryWorkflowService;
    }

    public void process(UUID taskId) {
        TaskExecutionSnapshot snapshot = processingTaskRepository.findExecutionSnapshotById(taskId).orElse(null);
        if (snapshot == null) {
            return;
        }
        ProcessingStage stage = snapshot.getCurrentStage();
        if (stage == ProcessingStage.SEARCH_COURIER) {
            courierSearchWorker.process(taskId);
            return;
        }
        try {
            deliveryWorkflowService.advanceDueDelivery(taskId, snapshot.getVersion());
        } catch (RuntimeException exception) {
            String details = "Delivery stage failed: " + exception.getClass().getSimpleName();
            log.warn("Task {} stage {}: {}", taskId, stage, details);
            deliveryWorkflowService.recordStageFailure(taskId, stage, snapshot.getVersion(), details);
        }
    }
}
