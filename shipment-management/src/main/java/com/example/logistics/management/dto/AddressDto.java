package com.example.logistics.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 255) String address
) {
}
