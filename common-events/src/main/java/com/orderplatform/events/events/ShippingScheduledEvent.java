package com.orderplatform.events.events;

import com.orderplatform.events.SagaMessage;

public record ShippingScheduledEvent(String sagaId, Long orderId, String shipmentId) implements SagaMessage {
}
