package com.orderplatform.order.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty @Valid List<Item> items,
        @NotBlank String shippingAddress
) {
    public record Item(
            @NotNull Long productId,
            @Positive int quantity,
            @NotNull BigDecimal unitPrice
    ) {
    }
}
