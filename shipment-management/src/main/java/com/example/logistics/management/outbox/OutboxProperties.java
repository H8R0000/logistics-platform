package com.example.logistics.management.outbox;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(@NotNull Duration publishInterval, @Min(1) @Max(100) int batchSize) {

    @AssertTrue(message = "outbox.publish-interval must be at least 1ms")
    public boolean isPublishIntervalPositive() {
        return publishInterval != null && !publishInterval.isNegative() && publishInterval.toMillis() >= 1;
    }
}
