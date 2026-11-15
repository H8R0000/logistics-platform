package com.example.logistics.processing.scheduler;

import com.example.logistics.processing.config.ProcessingProperties;
import com.example.logistics.processing.repository.ProcessingTaskRepository;
import com.example.logistics.processing.workflow.ProcessingTaskWorker;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Периодически передаёт готовые к выполнению задачи из PostgreSQL в Executor.
 * Поиск курьера использует захват с ограниченным сроком; этапы доставки — короткую блокирующую транзакцию.
 */
@Component
public class ProcessingTaskDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ProcessingTaskDispatcher.class);

    private final ProcessingTaskRepository processingTaskRepository;
    private final ProcessingTaskWorker processingTaskWorker;
    private final ThreadPoolTaskExecutor executor;
    private final ProcessingProperties processingProperties;
    /** ID задач в очереди и рабочих потоках этого экземпляра B; защиту между экземплярами обеспечивает БД. */
    private final Set<UUID> submitted = ConcurrentHashMap.newKeySet();

    public ProcessingTaskDispatcher(ProcessingTaskRepository processingTaskRepository, ProcessingTaskWorker processingTaskWorker,
                                    @Qualifier("processingExecutor") ThreadPoolTaskExecutor executor,
                                    ProcessingProperties processingProperties) {
        this.processingTaskRepository = processingTaskRepository;
        this.processingTaskWorker = processingTaskWorker;
        this.executor = executor;
        this.processingProperties = processingProperties;
    }

    @Scheduled(fixedDelayString = "${processing.dispatch-interval}")
    public void dispatch() {
        for (UUID id : processingTaskRepository.findDueTaskIds(Instant.now(),
            PageRequest.of(0, processingProperties.dispatchBatchSize()))) {
            if (!submitted.add(id)) {
                continue;
            }
            try {
                executor.execute(() -> {
                    try {
                        processingTaskWorker.process(id);
                    } catch (RuntimeException exception) {
                        log.error("Worker failed for task {}; PostgreSQL retains the task for recovery", id, exception);
                    } finally {
                        submitted.remove(id);
                    }
                });
            } catch (TaskRejectedException exception) {
                submitted.remove(id);
                log.info("Processing executor is full; remaining processingTaskRepository stay in PostgreSQL");
                break;
            }
        }
    }
}
