package com.orderplatform.events;

import java.math.BigDecimal;

/**
 * A single line of an order as it travels across services on the saga path.
 * Carries enough to reserve stock (productId, quantity) and price the order (unitPrice).
 */
public record OrderLineItem(
        Long productId,
        int quantity,
        BigDecimal unitPrice
) {
}
