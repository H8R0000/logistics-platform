package com.example.logistics.processing.entity;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ShipmentData(@NotNull @Valid Address origin, @NotNull @Valid Address destination,
                           @NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal weight,
                           @NotNull @Valid Dimensions dimensions, @NotNull DeliveryType deliveryType) {
}
