package com.example.logistics.courier.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("courier.simulation")
public record CourierSimulationProperties(
        @NotNull @DurationMin(millis = 1) Duration updateInterval,
        @DecimalMin("0") @DecimalMax("1") double availabilityProbability,
        @DecimalMin("0") @DecimalMax("1") double errorProbability,
        @NotNull @DurationMin @DurationMax(seconds = 30) Duration responseDelay) {
}
