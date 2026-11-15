package com.example.logistics.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.logistics.processing.dto.TaskResponse;
import com.example.logistics.processing.entity.Address;
import com.example.logistics.processing.entity.AssignmentStatus;
import com.example.logistics.processing.entity.CourierAssignmentEntity;
import com.example.logistics.processing.entity.DeliveryType;
import com.example.logistics.processing.entity.Dimensions;
import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.entity.RouteSheetEntity;
import com.example.logistics.processing.entity.RouteSheetStatus;
import com.example.logistics.processing.entity.ShipmentData;
import com.example.logistics.processing.exception.TaskRetryConflictException;
import com.example.logistics.processing.mapper.ProcessingTaskMapper;
import com.example.logistics.processing.repository.CourierAssignmentRepository;
import com.example.logistics.processing.repository.ProcessingTaskRepository;
import com.example.logistics.processing.repository.RouteSheetRepository;
import com.example.logistics.processing.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessingTaskAdminServiceTest {

    private static final Instant START = Instant.parse("2026-11-15T12:00:00Z");
    private static final CurrentUser ADMIN = new CurrentUser("admin-id", true);

    @Mock
    private ProcessingTaskRepository processingTaskRepository;
    @Mock
    private CourierAssignmentRepository courierAssignmentRepository;
    @Mock
    private RouteSheetRepository routeSheetRepository;
    @Mock
    private ProcessingTaskEventService processingTaskEventService;

    private ProcessingTaskAdminService service;
    private ProcessingTaskEntity task;

    @BeforeEach
    void setUp() {
        service = new ProcessingTaskAdminService(processingTaskRepository, courierAssignmentRepository,
                routeSheetRepository, processingTaskEventService, new ProcessingTaskMapper());
        Address address = new Address("Vilnius", "Lithuania", "Street 1");
        ShipmentData data = new ShipmentData(address, address, BigDecimal.ONE,
                new Dimensions(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE), DeliveryType.STANDARD);
        task = new ProcessingTaskEntity(UUID.randomUUID(), UUID.randomUUID(), "owner", data, START);
        when(processingTaskRepository.findForExecution(task.getId())).thenReturn(Optional.of(task));
    }

    @Test
    void retryAfterCourierSearchTimeoutStartsNewSearchWindow() {
        task.startSearch(START, START.plusSeconds(60));
        task.courierNotFound(START.plusSeconds(300));
        Instant beforeRetry = Instant.now();

        TaskResponse result = service.retry(task.getId(), ADMIN);

        assertThat(result.id()).isEqualTo(task.getId());
        assertThat(result.status()).isEqualTo(ProcessingTaskStatus.QUEUED);
        assertThat(result.currentStage()).isEqualTo(ProcessingStage.SEARCH_COURIER);
        assertThat(result.startedAt()).isNull();
        assertThat(result.completedAt()).isNull();
        assertThat(result.nextExecutionAt()).isBetween(beforeRetry, Instant.now());
        verify(processingTaskEventService).saveTaskEvent(eq(task), eq("TASK_RETRY_REQUESTED"),
                contains(ADMIN.id()), any(Instant.class));
    }

    @Test
    void retryDeliveryResumesConfirmationAndPreservesCourierAndRoute() {
        task.startSearch(START, START.plusSeconds(60));
        task.assignCourier(START);
        task.startDelivery(START.plusSeconds(120));
        task.recordFailure("Temporary failure");
        task.fail(START.plusSeconds(121));
        UUID courierId = UUID.randomUUID();
        CourierAssignmentEntity assignment = mock(CourierAssignmentEntity.class);
        when(assignment.getCourierId()).thenReturn(courierId);
        RouteSheetEntity route = new RouteSheetEntity(UUID.randomUUID(), task.getShipmentId(), courierId,
                task.getShipmentData().origin(), task.getShipmentData().destination(), Duration.ofMinutes(2), START);
        when(courierAssignmentRepository.findByShipmentIdAndStatus(task.getShipmentId(), AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(routeSheetRepository.findByShipmentId(task.getShipmentId())).thenReturn(Optional.of(route));

        TaskResponse result = service.retry(task.getId(), ADMIN);

        assertThat(result.status()).isEqualTo(ProcessingTaskStatus.IN_TRANSIT);
        assertThat(result.currentStage()).isEqualTo(ProcessingStage.CONFIRM_DELIVERY);
        assertThat(result.startedAt()).isEqualTo(START);
        assertThat(result.attempts()).isZero();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.completedAt()).isNull();
        assertThat(route.getStatus()).isEqualTo(RouteSheetStatus.IN_TRANSIT);
        assertThat(route.getEstimatedDeliveryAt()).isEqualTo(START.plusSeconds(120));
        verify(courierAssignmentRepository).findByShipmentIdAndStatus(task.getShipmentId(), AssignmentStatus.ACTIVE);
        verify(routeSheetRepository).findByShipmentId(task.getShipmentId());
        verifyNoMoreInteractions(courierAssignmentRepository, routeSheetRepository);
    }

    @Test
    void completedDeliveryCannotBeRetried() {
        task.startDelivery(START.plusSeconds(120));
        task.completeDelivery(START.plusSeconds(120));

        assertThatThrownBy(() -> service.retry(task.getId(), ADMIN))
                .isInstanceOf(TaskRetryConflictException.class);

        assertThat(task.getStatus()).isEqualTo(ProcessingTaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isEqualTo(START.plusSeconds(120));
        verifyNoInteractions(courierAssignmentRepository, routeSheetRepository, processingTaskEventService);
    }
}
