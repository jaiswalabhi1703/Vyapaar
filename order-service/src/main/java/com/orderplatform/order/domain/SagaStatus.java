package com.orderplatform.order.domain;

/**
 * Saga state machine. The forward path is the first five values in order; the index of the
 * furthest-reached forward state tells the orchestrator which compensations to fire.
 */
public enum SagaStatus {
    STARTED,
    INVENTORY_RESERVED,
    PAYMENT_AUTHORIZED,
    SHIPPING_SCHEDULED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED
}
