package com.orderplatform.payment.domain;

public enum PaymentStatus {
    /** PayU checkout created; awaiting the customer to complete payment and PayU to call back. */
    PENDING,
    AUTHORIZED,
    DECLINED,
    REFUNDED
}
