package com.example.logistics.processing.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Интервалы условной длительности доставки; значения находятся в application.yml, а не в env. */
@Validated
@ConfigurationProperties("delivery-simulation")
public record DeliverySimulationProperties(
        @NotNull Duration standardMinDuration, @NotNull Duration standardMaxDuration,
        @NotNull Duration expressMinDuration, @NotNull Duration expressMaxDuration) {

    @AssertTrue(message = "delivery duration ranges must satisfy 1ms <= min <= max <= 1d")
    public boolean isRangeValid() {
        return valid(standardMinDuration, standardMaxDuration) && valid(expressMinDuration, expressMaxDuration);
    }

    private static boolean valid(Duration min, Duration max) {
        return min != null && max != null && min.compareTo(Duration.ofMillis(1)) >= 0
                && max.compareTo(min) >= 0 && max.compareTo(Duration.ofDays(1)) <= 0;
    }
}
