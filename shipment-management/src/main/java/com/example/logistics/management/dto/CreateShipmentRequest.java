package com.example.logistics.management.dto;

import com.example.logistics.management.entity.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateShipmentRequest(
        @NotNull @Valid AddressDto origin,
        @NotNull @Valid AddressDto destination,
        @NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal weight,
        @NotNull @Valid DimensionsDto dimensions,
        @NotNull DeliveryType deliveryType
) {
}
