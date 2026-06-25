package com.orderplatform.events.commands;

import com.orderplatform.events.OrderLineItem;
import com.orderplatform.events.SagaMessage;

import java.util.List;

/** Compensation: undoes a previous ReserveInventoryCommand. */
public record ReleaseInventoryCommand(String sagaId, Long orderId, List<OrderLineItem> items)
        implements SagaMessage {
}
