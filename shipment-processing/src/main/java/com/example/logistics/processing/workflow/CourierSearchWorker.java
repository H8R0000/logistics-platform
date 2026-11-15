package com.example.logistics.processing.workflow;

import com.example.logistics.processing.client.Courier;
import com.example.logistics.processing.client.CourierClient;
import com.example.logistics.processing.entity.AssignmentStatus;
import com.example.logistics.processing.repository.CourierAssignmentRepository;
import com.example.logistics.processing.service.CourierSearchStateService;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * Выполняет один запуск поиска курьера: захват задачи, запрос C, выбор и сохранение результата.
 * HTTP-запрос идёт вне транзакции; изменения БД выполняет {@link CourierSearchStateService}.
 */
@Component
public class CourierSearchWorker {

    private static final Logger log = LoggerFactory.getLogger(CourierSearchWorker.class);

    private final CourierSearchStateService courierSearchStateService;
    private final CourierClient courierClient;
    private final CourierSelector courierSelector;
    private final CourierAssignmentRepository courierAssignmentRepository;

    public CourierSearchWorker(CourierSearchStateService courierSearchStateService, CourierClient courierClient,
                               CourierSelector courierSelector, CourierAssignmentRepository courierAssignmentRepository) {
        this.courierSearchStateService = courierSearchStateService;
        this.courierClient = courierClient;
        this.courierSelector = courierSelector;
        this.courierAssignmentRepository = courierAssignmentRepository;
    }

    public void process(UUID id) {
        ClaimedTask claim = courierSearchStateService.claim(id).orElse(null);
        if (claim == null) {
            return;
        }
        log.info("Searching courier for task {}", id);
        try {
            List<Courier> candidates = courierSelector.suitable(courierClient.getAvailableCouriers(), claim.shipmentData().weight(),
                new HashSet<>(courierAssignmentRepository.findCourierIdsByStatus(AssignmentStatus.ACTIVE)));
            for (Courier courier : candidates) {
                CourierSearchStateService.AssignmentResult result = courierSearchStateService.assign(claim, courier.id());
                if (result != CourierSearchStateService.AssignmentResult.BUSY) {
                    log.info("Courier search for task {} finished with {}", id, result);
                    return;
                }
            }
            courierSearchStateService.waitForCourier(claim);
        } catch (RuntimeException exception) {
            String details = exception instanceof RestClientResponseException response
                ? "Courier request failed: HTTP " + response.getStatusCode().value()
                : "Courier search failed: " + exception.getClass().getSimpleName();
            log.warn("Task {}: {}", id, details);
            courierSearchStateService.recordFailure(claim, details);
        }
    }
}
