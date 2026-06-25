package com.orderplatform.inventory.web.dto;

import com.orderplatform.inventory.domain.Product;
import com.orderplatform.inventory.domain.Stock;

import java.math.BigDecimal;

/**
 * Catalog item. Exact stock numbers ({@code availableQty}/{@code reservedQty}) are only
 * populated for admins; normal shoppers just see the {@code inStock} flag.
 */
public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        boolean inStock,
        Integer availableQty,
        Integer reservedQty
) {
    public static ProductResponse of(Product p, Stock s, boolean includeStock) {
        int available = s != null ? s.getAvailableQty() : 0;
        Integer avail = includeStock ? available : null;
        Integer reserved = includeStock ? (s != null ? s.getReservedQty() : 0) : null;
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getDescription(),
                p.getImageUrl(), available > 0, avail, reserved);
    }
}
