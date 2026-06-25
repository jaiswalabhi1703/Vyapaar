package com.orderplatform.order.web.dto;

import com.orderplatform.order.domain.OrderEntity;
import com.orderplatform.order.domain.SagaState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Everything the frontend needs to render an order and its live saga stepper. */
public record OrderResponse(
        Long id,
        String userId,
        String status,
        BigDecimal total,
        String shippingAddress,
        List<Line> items,
        Saga saga,
        Instant createdAt,
        Instant updatedAt
) {
    public record Line(Long productId, int quantity, BigDecimal unitPrice) {
    }

    public record Saga(String status, String reachedStatus, String currentStep,
                       String failedStep, String failureReason) {
    }

    public static OrderResponse of(OrderEntity o, SagaState s) {
        List<Line> lines = o.getItems().stream()
                .map(i -> new Line(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        Saga saga = s == null ? null : new Saga(
                s.getStatus().name(),
                s.getReachedStatus().name(),
                s.getCurrentStep(),
                s.getFailedStep(),
                s.getFailureReason());
        return new OrderResponse(o.getId(), o.getUserId(), o.getStatus().name(), o.getTotal(),
                o.getShippingAddress(), lines, saga, o.getCreatedAt(), o.getUpdatedAt());
    }
}
