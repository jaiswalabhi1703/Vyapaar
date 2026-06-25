package com.orderplatform.events.commands;

import com.orderplatform.events.SagaMessage;

import java.math.BigDecimal;

public record AuthorizePaymentCommand(String sagaId, Long orderId, BigDecimal amount, String idempotencyKey)
        implements SagaMessage {
}
