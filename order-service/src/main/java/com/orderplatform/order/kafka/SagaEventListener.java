package com.orderplatform.order.kafka;

import com.orderplatform.events.SagaMessage;
import com.orderplatform.events.Topics;
import com.orderplatform.events.events.InventoryReservationFailedEvent;
import com.orderplatform.events.events.InventoryReservedEvent;
import com.orderplatform.events.events.PaymentAuthorizedEvent;
import com.orderplatform.events.events.PaymentFailedEvent;
import com.orderplatform.events.events.ShippingFailedEvent;
import com.orderplatform.events.events.ShippingScheduledEvent;
import com.orderplatform.order.saga.SagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the business services' events and drives the orchestrator. One listener method
 * per topic; the JsonDeserializer reconstructs the concrete event type from the type header.
 */
@Component
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    private final SagaOrchestrator orchestrator;

    public SagaEventListener(SagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "order-service")
    public void onInventoryEvent(SagaMessage event) {
        MDC.put("sagaId", event.sagaId());
        try {
            if (event instanceof InventoryReservedEvent e) {
                orchestrator.onInventoryReserved(e.sagaId(), e.orderId());
            } else if (event instanceof InventoryReservationFailedEvent e) {
                orchestrator.onInventoryFailed(e.sagaId(), e.orderId(), e.reason());
            } else {
                log.warn("unexpected inventory event: {}", event);
            }
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "order-service")
    public void onPaymentEvent(SagaMessage event) {
        MDC.put("sagaId", event.sagaId());
        try {
            if (event instanceof PaymentAuthorizedEvent e) {
                orchestrator.onPaymentAuthorized(e.sagaId(), e.orderId());
            } else if (event instanceof PaymentFailedEvent e) {
                orchestrator.onPaymentFailed(e.sagaId(), e.orderId(), e.reason());
            } else {
                log.warn("unexpected payment event: {}", event);
            }
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = Topics.SHIPPING_EVENTS, groupId = "order-service")
    public void onShippingEvent(SagaMessage event) {
        MDC.put("sagaId", event.sagaId());
        try {
            if (event instanceof ShippingScheduledEvent e) {
                orchestrator.onShippingScheduled(e.sagaId(), e.orderId());
            } else if (event instanceof ShippingFailedEvent e) {
                orchestrator.onShippingFailed(e.sagaId(), e.orderId(), e.reason());
            } else {
                log.warn("unexpected shipping event: {}", event);
            }
        } finally {
            MDC.clear();
        }
    }
}
