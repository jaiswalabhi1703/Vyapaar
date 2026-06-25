package com.orderplatform.inventory.service;

import com.orderplatform.events.OrderLineItem;
import com.orderplatform.events.Topics;
import com.orderplatform.events.commands.ReleaseInventoryCommand;
import com.orderplatform.events.commands.ReserveInventoryCommand;
import com.orderplatform.events.events.InventoryReservationFailedEvent;
import com.orderplatform.events.events.InventoryReservedEvent;
import com.orderplatform.inventory.domain.Product;
import com.orderplatform.inventory.domain.ProcessedMessage;
import com.orderplatform.inventory.domain.Stock;
import com.orderplatform.inventory.repo.ProcessedMessageRepository;
import com.orderplatform.inventory.repo.ProductRepository;
import com.orderplatform.inventory.repo.StockRepository;
import com.orderplatform.inventory.web.dto.CreateProductRequest;
import com.orderplatform.inventory.web.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository products;
    private final StockRepository stocks;
    private final ProcessedMessageRepository processed;
    private final KafkaTemplate<String, Object> kafka;

    public InventoryService(ProductRepository products, StockRepository stocks,
                            ProcessedMessageRepository processed, KafkaTemplate<String, Object> kafka) {
        this.products = products;
        this.stocks = stocks;
        this.processed = processed;
        this.kafka = kafka;
    }

    // ---------- Read / admin (Phase 1) ----------

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(boolean includeStock) {
        return products.findAll().stream()
                .map(p -> ProductResponse.of(p, stocks.findById(p.getId()).orElse(null), includeStock))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id, boolean includeStock) {
        Product p = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No product " + id));
        return ProductResponse.of(p, stocks.findById(id).orElse(null), includeStock);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req) {
        Product saved = products.save(new Product(req.name(), req.price(), req.description(), req.imageUrl()));
        stocks.save(new Stock(saved.getId(), req.initialStock()));
        return ProductResponse.of(saved, stocks.findById(saved.getId()).orElse(null), true);
    }

    // ---------- Saga participation (Phase 2/3/4) ----------

    /**
     * Reserve stock for every line of the order. Idempotent on (sagaId + command type):
     * a redelivered command is a no-op. On insufficient stock, publishes a failure event
     * and the orchestrator compensates.
     */
    @Transactional
    public void handleReserve(ReserveInventoryCommand cmd) {
        if (alreadyProcessed(cmd.sagaId(), cmd.messageType())) {
            log.info("[saga {}] duplicate ReserveInventoryCommand ignored", cmd.sagaId());
            return;
        }
        try {
            for (OrderLineItem item : cmd.items()) {
                Stock stock = stocks.findById(item.productId())
                        .orElseThrow(() -> new IllegalArgumentException("No stock row for product " + item.productId()));
                stock.reserve(item.quantity());
                stocks.save(stock);
            }
            markProcessed(cmd.sagaId(), cmd.messageType());
            log.info("[saga {}] inventory reserved for order {}", cmd.sagaId(), cmd.orderId());
            kafka.send(Topics.INVENTORY_EVENTS, cmd.sagaId(),
                    new InventoryReservedEvent(cmd.sagaId(), cmd.orderId()));
        } catch (RuntimeException ex) {
            // Roll back the reservation work but still report the failure to the orchestrator.
            log.warn("[saga {}] inventory reservation failed: {}", cmd.sagaId(), ex.getMessage());
            throw new ReservationFailure(cmd, ex.getMessage());
        }
    }

    /** Compensation: release everything this saga reserved. Idempotent. */
    @Transactional
    public void handleRelease(ReleaseInventoryCommand cmd) {
        if (alreadyProcessed(cmd.sagaId(), cmd.messageType())) {
            log.info("[saga {}] duplicate ReleaseInventoryCommand ignored", cmd.sagaId());
            return;
        }
        for (OrderLineItem item : cmd.items()) {
            stocks.findById(item.productId()).ifPresent(stock -> {
                stock.release(item.quantity());
                stocks.save(stock);
            });
        }
        markProcessed(cmd.sagaId(), cmd.messageType());
        log.info("[saga {}] inventory released for order {}", cmd.sagaId(), cmd.orderId());
    }

    /** Published in its own (new) transaction after the reserve transaction rolled back. */
    public void publishReservationFailed(ReserveInventoryCommand cmd, String reason) {
        kafka.send(Topics.INVENTORY_EVENTS, cmd.sagaId(),
                new InventoryReservationFailedEvent(cmd.sagaId(), cmd.orderId(), reason));
    }

    private boolean alreadyProcessed(String sagaId, String type) {
        return processed.existsById(key(sagaId, type));
    }

    private void markProcessed(String sagaId, String type) {
        processed.save(new ProcessedMessage(key(sagaId, type)));
    }

    private static String key(String sagaId, String type) {
        return sagaId + ":" + type;
    }

    /** Signals that reservation work must roll back; carries data needed to emit the failure event. */
    public static class ReservationFailure extends RuntimeException {
        public final transient ReserveInventoryCommand command;
        public final String reason;

        ReservationFailure(ReserveInventoryCommand command, String reason) {
            super(reason);
            this.command = command;
            this.reason = reason;
        }
    }
}
