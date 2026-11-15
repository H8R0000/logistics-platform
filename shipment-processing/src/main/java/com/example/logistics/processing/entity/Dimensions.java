package com.example.logistics.processing.entity;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record Dimensions(@NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal length,
                         @NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal width,
                         @NotNull @DecimalMin("0.001") @Digits(integer = 12, fraction = 3) BigDecimal height) {
}
