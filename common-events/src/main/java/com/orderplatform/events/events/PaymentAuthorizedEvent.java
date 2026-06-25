package com.orderplatform.events.events;

import com.orderplatform.events.SagaMessage;

public record PaymentAuthorizedEvent(String sagaId, Long orderId, String paymentId) implements SagaMessage {
}
