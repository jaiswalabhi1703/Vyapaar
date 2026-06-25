package com.orderplatform.shipping.service;

import com.orderplatform.events.Topics;
import com.orderplatform.events.commands.CancelShippingCommand;
import com.orderplatform.events.commands.ScheduleShippingCommand;
import com.orderplatform.events.events.ShippingFailedEvent;
import com.orderplatform.events.events.ShippingScheduledEvent;
import com.orderplatform.shipping.domain.Shipment;
import com.orderplatform.shipping.repo.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final ShipmentRepository shipments;
    private final KafkaTemplate<String, Object> kafka;

    public ShippingService(ShipmentRepository shipments, KafkaTemplate<String, Object> kafka) {
        this.shipments = shipments;
        this.kafka = kafka;
    }

    /**
     * Schedule dispatch. Idempotent on orderId (one shipment per order). An address
     * containing "FAIL" injects a shipping failure so the last step's compensation
     * (refund + release) can be demonstrated.
     */
    @Transactional
    public void handleSchedule(ScheduleShippingCommand cmd) {
        var existing = shipments.findByOrderId(cmd.orderId());
        if (existing.isPresent()) {
            log.info("[saga {}] duplicate ScheduleShippingCommand -> replaying scheduled", cmd.sagaId());
            kafka.send(Topics.SHIPPING_EVENTS, cmd.sagaId(),
                    new ShippingScheduledEvent(cmd.sagaId(), cmd.orderId(),
                            String.valueOf(existing.get().getId())));
            return;
        }

        if (cmd.address() != null && cmd.address().toUpperCase().contains("FAIL")) {
            log.warn("[saga {}] shipping failed for order {} (injected)", cmd.sagaId(), cmd.orderId());
            kafka.send(Topics.SHIPPING_EVENTS, cmd.sagaId(),
                    new ShippingFailedEvent(cmd.sagaId(), cmd.orderId(), "no carrier available"));
            return;
        }

        Shipment shipment = shipments.save(new Shipment(cmd.orderId(), cmd.address()));
        log.info("[saga {}] shipment {} scheduled for order {}",
                cmd.sagaId(), shipment.getId(), cmd.orderId());
        kafka.send(Topics.SHIPPING_EVENTS, cmd.sagaId(),
                new ShippingScheduledEvent(cmd.sagaId(), cmd.orderId(), String.valueOf(shipment.getId())));
    }

    /** Compensation: cancel the shipment for this order. Idempotent. */
    @Transactional
    public void handleCancel(CancelShippingCommand cmd) {
        shipments.findByOrderId(cmd.orderId()).ifPresent(shipment -> {
            shipment.cancel();
            shipments.save(shipment);
            log.info("[saga {}] shipment cancelled for order {}", cmd.sagaId(), cmd.orderId());
        });
    }
}
