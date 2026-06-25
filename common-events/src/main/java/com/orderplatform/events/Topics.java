package com.orderplatform.events;

/**
 * Central registry of Kafka topic names so producers and consumers never drift.
 * One topic per command/event family keeps the wiring obvious in Kafka UI / logs.
 */
public final class Topics {

    private Topics() {
    }

    // Commands: orchestrator -> business services
    public static final String RESERVE_INVENTORY = "inventory.reserve.command";
    public static final String RELEASE_INVENTORY = "inventory.release.command";
    public static final String AUTHORIZE_PAYMENT = "payment.authorize.command";
    public static final String REFUND_PAYMENT = "payment.refund.command";
    public static final String SCHEDULE_SHIPPING = "shipping.schedule.command";
    public static final String CANCEL_SHIPPING = "shipping.cancel.command";

    // Events: business services -> orchestrator
    public static final String INVENTORY_EVENTS = "inventory.events";
    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String SHIPPING_EVENTS = "shipping.events";

    // Phase 0 smoke test
    public static final String SMOKE_TEST = "smoke.test";
}
