package com.example.logistics.management.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Один доверенный внешний источник, имеющий отдельное право Write на shipment-requests. */
@Validated
@ConfigurationProperties(prefix = "external-shipments")
public record ExternalShipmentProperties(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}") String sourceId,
        @NotBlank @Size(max = 255) @Pattern(regexp = "external:[A-Za-z0-9._-]+") String ownerId) {
}
