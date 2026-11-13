package com.example.logistics.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DimensionsDto(
        @NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal length,
        @NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal width,
        @NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal height
) {
}
