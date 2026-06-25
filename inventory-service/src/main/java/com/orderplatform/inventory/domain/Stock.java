package com.orderplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Stock for a product. Keyed by productId (one row per product).
 * The {@code @Version} column gives optimistic locking so two concurrent orders for the
 * last unit cannot both succeed (Phase 4 concurrency control).
 */
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Version
    private long version;

    protected Stock() {
    }

    public Stock(Long productId, int availableQty) {
        this.productId = productId;
        this.availableQty = availableQty;
        this.reservedQty = 0;
    }

    /** Move {@code qty} from available to reserved. Throws if not enough on hand. */
    public void reserve(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (availableQty < qty) {
            throw new InsufficientStockException(productId, qty, availableQty);
        }
        availableQty -= qty;
        reservedQty += qty;
    }

    /** Move {@code qty} back from reserved to available (compensation / release). */
    public void release(int qty) {
        int toRelease = Math.min(qty, reservedQty);
        reservedQty -= toRelease;
        availableQty += toRelease;
    }

    public Long getProductId() {
        return productId;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    public int getReservedQty() {
        return reservedQty;
    }
}
