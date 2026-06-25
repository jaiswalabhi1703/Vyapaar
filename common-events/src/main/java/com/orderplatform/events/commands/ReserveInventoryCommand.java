package com.orderplatform.events.commands;

import com.orderplatform.events.OrderLineItem;
import com.orderplatform.events.SagaMessage;

import java.util.List;

public record ReserveInventoryCommand(String sagaId, Long orderId, List<OrderLineItem> items)
        implements SagaMessage {
}
