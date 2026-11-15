package com.example.logistics.processing.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Настройки раздела {@code processing} из application.yml; проверяются при запуске приложения.
 *
 * @param executor настройки пула потоков
 * @param dispatchInterval пауза между запусками диспетчера
 * @param dispatchBatchSize максимальное число ID в одной выборке диспетчера
 * @param executionTimeout срок одного выполнения, после которого задачу можно захватить повторно
 * @param courierSearchInterval пауза перед повторным поиском при отсутствии курьеров
 * @param maxCourierWait общий срок поиска от первого startedAt, включая ожидание и повторы
 * @param maxAttempts лимит последовательных технических неудач
 * @param retryInitialDelay начальная пауза технического повтора
 * @param retryMaxDelay максимальная пауза технического повтора
 */
@Validated
@ConfigurationProperties("processing")
public record ProcessingProperties(
    @NotNull @Valid Pool executor,
    @NotNull Duration dispatchInterval,
    @Min(1) @Max(100) int dispatchBatchSize,
    @NotNull Duration executionTimeout,
    @NotNull Duration courierSearchInterval,
    @NotNull Duration maxCourierWait,
    @Min(1) @Max(10) int maxAttempts,
    @NotNull Duration retryInitialDelay,
    @NotNull Duration retryMaxDelay) {

    @AssertTrue(message = "processing durations must be positive milliseconds; retry-max-delay >= retry-initial-delay")
    public boolean isTimingValid() {
        return positive(dispatchInterval) && positive(executionTimeout) && positive(courierSearchInterval)
            && positive(maxCourierWait) && positive(retryInitialDelay) && positive(retryMaxDelay)
            && retryMaxDelay.compareTo(retryInitialDelay) >= 0;
    }

    private static boolean positive(Duration value) {
        return value != null && !value.isNegative() && value.toMillis() >= 1;
    }

    /** Значения раздела {@code processing.executor}; сам пул создаёт {@link ProcessingConfiguration}. */
    public record Pool(@Min(1) @Max(32) int corePoolSize, @Min(1) @Max(64) int maxPoolSize,
                       @Min(1) @Max(1000) int queueCapacity, @NotNull RejectionPolicy rejectionPolicy) {

        @AssertTrue(message = "max-pool-size must be >= core-pool-size")
        public boolean isPoolSizeValid() {
            return maxPoolSize >= corePoolSize;
        }
    }

    public enum RejectionPolicy {
        ABORT, CALLER_RUNS
    }
}
