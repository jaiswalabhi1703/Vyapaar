package com.orderplatform.order.saga;

import com.orderplatform.events.OrderLineItem;
import com.orderplatform.events.Topics;
import com.orderplatform.events.commands.AuthorizePaymentCommand;
import com.orderplatform.events.commands.CancelShippingCommand;
import com.orderplatform.events.commands.ReleaseInventoryCommand;
import com.orderplatform.events.commands.RefundPaymentCommand;
import com.orderplatform.events.commands.ReserveInventoryCommand;
import com.orderplatform.events.commands.ScheduleShippingCommand;
import com.orderplatform.order.domain.OrderEntity;
import com.orderplatform.order.domain.OrderStatus;
import com.orderplatform.order.domain.ProcessedEvent;
import com.orderplatform.order.domain.SagaState;
import com.orderplatform.order.domain.SagaStatus;
import com.orderplatform.order.repo.OrderRepository;
import com.orderplatform.order.repo.ProcessedEventRepository;
import com.orderplatform.order.repo.SagaStateRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The saga orchestrator: the brain of the platform. It persists saga state on every
 * transition (so it survives a crash), reacts to each event by firing the next command via
 * the outbox, and on any failure unwinds the already-completed steps in reverse order.
 *
 * <p>Forward path: STARTED → INVENTORY_RESERVED → PAYMENT_AUTHORIZED → SHIPPING_SCHEDULED
 * → COMPLETED. Any *_FAILED diverts to COMPENSATING → COMPENSATED (order CANCELLED).
 */
@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final SagaStateRepository sagas;
    private final OrderRepository orders;
    private final ProcessedEventRepository processed;
    private final OutboxPublisher outbox;
    private final Counter startedCounter;
    private final Counter completedCounter;
    private final Counter compensatedCounter;

    public SagaOrchestrator(SagaStateRepository sagas, OrderRepository orders,
                            ProcessedEventRepository processed, OutboxPublisher outbox,
                            MeterRegistry meters) {
        this.sagas = sagas;
        this.orders = orders;
        this.processed = processed;
        this.outbox = outbox;
        this.startedCounter = Counter.builder("saga.started").description("orders that started a saga").register(meters);
        this.completedCounter = Counter.builder("saga.completed").description("sagas that completed successfully").register(meters);
        this.compensatedCounter = Counter.builder("saga.compensated").description("sagas that were compensated").register(meters);
    }

    // ---------- Start (called within the place-order transaction) ----------

    public void start(OrderEntity order) {
        String sagaId = "saga-" + order.getId();
        MDC.put("sagaId", sagaId);
        sagas.save(new SagaState(sagaId, order.getId()));
        startedCounter.increment();
        log.info("[saga {}] started for order {} (total {})", sagaId, order.getId(), order.getTotal());
        outbox.enqueue(Topics.RESERVE_INVENTORY, sagaId,
                new ReserveInventoryCommand(sagaId, order.getId(), lineItems(order)));
    }

    // ---------- Forward transitions ----------

    @Transactional
    public void onInventoryReserved(String sagaId, Long orderId) {
        if (duplicate(sagaId, "InventoryReservedEvent")) return;
        SagaState saga = require(sagaId);
        if (saga.isCompensating() || saga.isTerminal()) return;
        saga.advance(SagaStatus.INVENTORY_RESERVED, "AUTHORIZE_PAYMENT");
        OrderEntity order = order(orderId);
        order.transitionTo(OrderStatus.INVENTORY_RESERVED);
        log.info("[saga {}] inventory reserved -> authorizing payment", sagaId);
        outbox.enqueue(Topics.AUTHORIZE_PAYMENT, sagaId,
                new AuthorizePaymentCommand(sagaId, orderId, order.getTotal(), sagaId));
    }

    @Transactional
    public void onPaymentAuthorized(String sagaId, Long orderId) {
        if (duplicate(sagaId, "PaymentAuthorizedEvent")) return;
        SagaState saga = require(sagaId);
        if (saga.isCompensating() || saga.isTerminal()) return;
        saga.advance(SagaStatus.PAYMENT_AUTHORIZED, "SCHEDULE_SHIPPING");
        OrderEntity order = order(orderId);
        order.transitionTo(OrderStatus.PAYMENT_AUTHORIZED);
        log.info("[saga {}] payment authorized -> scheduling shipping", sagaId);
        outbox.enqueue(Topics.SCHEDULE_SHIPPING, sagaId,
                new ScheduleShippingCommand(sagaId, orderId, order.getShippingAddress()));
    }

    @Transactional
    public void onShippingScheduled(String sagaId, Long orderId) {
        if (duplicate(sagaId, "ShippingScheduledEvent")) return;
        SagaState saga = require(sagaId);
        if (saga.isCompensating() || saga.isTerminal()) return;
        saga.advance(SagaStatus.COMPLETED, "DONE");
        order(orderId).transitionTo(OrderStatus.COMPLETED);
        completedCounter.increment();
        log.info("[saga {}] shipping scheduled -> order COMPLETED", sagaId);
    }

    // ---------- Failure transitions ----------

    @Transactional
    public void onInventoryFailed(String sagaId, Long orderId, String reason) {
        if (duplicate(sagaId, "InventoryReservationFailedEvent")) return;
        compensate(sagaId, orderId, "INVENTORY", reason);
    }

    @Transactional
    public void onPaymentFailed(String sagaId, Long orderId, String reason) {
        if (duplicate(sagaId, "PaymentFailedEvent")) return;
        compensate(sagaId, orderId, "PAYMENT", reason);
    }

    @Transactional
    public void onShippingFailed(String sagaId, Long orderId, String reason) {
        if (duplicate(sagaId, "ShippingFailedEvent")) return;
        compensate(sagaId, orderId, "SHIPPING", reason);
    }

    // ---------- Compensation ----------

    /**
     * Unwind the steps already completed, in reverse order, based on the furthest forward
     * state the saga reached. Also used by the timeout scanner when a step never responds.
     */
    @Transactional
    public void compensate(String sagaId, Long orderId, String failedStep, String reason) {
        SagaState saga = require(sagaId);
        if (saga.isCompensating() || saga.isTerminal()) {
            return; // already unwinding or finished
        }
        SagaStatus reached = saga.getReachedStatus();
        saga.fail(failedStep, reason);
        OrderEntity order = order(orderId);
        order.transitionTo(OrderStatus.COMPENSATING);
        log.warn("[saga {}] {} failed ({}) — compensating from {}", sagaId, failedStep, reason, reached);

        // Reverse order: refund payment (if authorized) then release inventory (if reserved).
        if (reached.ordinal() >= SagaStatus.PAYMENT_AUTHORIZED.ordinal()) {
            outbox.enqueue(Topics.REFUND_PAYMENT, sagaId, new RefundPaymentCommand(sagaId, orderId));
        }
        if (reached.ordinal() >= SagaStatus.INVENTORY_RESERVED.ordinal()) {
            outbox.enqueue(Topics.RELEASE_INVENTORY, sagaId,
                    new ReleaseInventoryCommand(sagaId, orderId, lineItems(order)));
        }
        // Shipping is never the step that needs undoing here: if shipping failed it produced no
        // shipment, and if a later step failed there is no later step. CancelShipping is wired
        // for completeness should the flow ever gain a post-shipping step.
        if (reached.ordinal() > SagaStatus.SHIPPING_SCHEDULED.ordinal()) {
            outbox.enqueue(Topics.CANCEL_SHIPPING, sagaId, new CancelShippingCommand(sagaId, orderId));
        }

        saga.markCompensated();
        order.transitionTo(OrderStatus.CANCELLED);
        compensatedCounter.increment();
        log.warn("[saga {}] compensated — order {} CANCELLED", sagaId, orderId);
    }

    // ---------- helpers ----------

    private boolean duplicate(String sagaId, String eventType) {
        String key = sagaId + ":" + eventType;
        if (processed.existsById(key)) {
            log.info("[saga {}] duplicate {} ignored", sagaId, eventType);
            return true;
        }
        processed.save(new ProcessedEvent(key));
        return false;
    }

    private SagaState require(String sagaId) {
        return sagas.findById(sagaId)
                .orElseThrow(() -> new IllegalStateException("No saga " + sagaId));
    }

    private OrderEntity order(Long orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("No order " + orderId));
    }

    private static List<OrderLineItem> lineItems(OrderEntity order) {
        return order.getItems().stream()
                .map(i -> new OrderLineItem(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
    }
}
