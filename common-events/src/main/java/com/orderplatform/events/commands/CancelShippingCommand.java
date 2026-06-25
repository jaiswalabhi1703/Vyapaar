package com.orderplatform.events.commands;

import com.orderplatform.events.SagaMessage;

/** Compensation: undoes a previous ScheduleShippingCommand. */
public record CancelShippingCommand(String sagaId, Long orderId) implements SagaMessage {
}
