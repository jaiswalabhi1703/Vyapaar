package com.orderplatform.inventory.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price,
        String description,
        String imageUrl,
        @Min(0) int initialStock
) {
}
