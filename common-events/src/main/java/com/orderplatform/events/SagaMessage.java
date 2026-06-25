package com.orderplatform.events;

/**
 * Every command and event on the saga path carries a sagaId (correlation id) and the
 * orderId it belongs to, so the whole journey of one order is traceable and dedupable.
 */
public interface SagaMessage {
    String sagaId();

    Long orderId();

    /** Stable discriminator used for idempotency keys (sagaId + type). */
    default String messageType() {
        return getClass().getSimpleName();
    }
}
