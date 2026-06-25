package com.orderplatform.events.commands;

import com.orderplatform.events.SagaMessage;

public record ScheduleShippingCommand(String sagaId, Long orderId, String address) implements SagaMessage {
}
