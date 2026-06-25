package com.orderplatform.events.events;

import com.orderplatform.events.SagaMessage;

public record ShippingFailedEvent(String sagaId, Long orderId, String reason) implements SagaMessage {
}
