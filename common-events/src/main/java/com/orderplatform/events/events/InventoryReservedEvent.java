package com.orderplatform.events.events;

import com.orderplatform.events.SagaMessage;

public record InventoryReservedEvent(String sagaId, Long orderId) implements SagaMessage {
}
