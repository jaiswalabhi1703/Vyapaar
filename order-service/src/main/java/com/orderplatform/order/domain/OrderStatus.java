package com.orderplatform.order.domain;

/**
 * Order lifecycle as the frontend stepper sees it. The happy path walks down the first
 * four; any failure diverts to COMPENSATING and ends at CANCELLED.
 */
public enum OrderStatus {
    CREATED,
    INVENTORY_RESERVED,
    PAYMENT_AUTHORIZED,
    SHIPPING_SCHEDULED,
    COMPLETED,
    COMPENSATING,
    CANCELLED
}
