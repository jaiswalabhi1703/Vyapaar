package com.orderplatform.events.commands;

import com.orderplatform.events.SagaMessage;

/** Compensation: undoes a previous AuthorizePaymentCommand. */
public record RefundPaymentCommand(String sagaId, Long orderId) implements SagaMessage {
}
