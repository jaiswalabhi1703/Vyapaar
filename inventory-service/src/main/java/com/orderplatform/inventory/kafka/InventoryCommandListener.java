package com.orderplatform.inventory.kafka;

import com.orderplatform.events.Topics;
import com.orderplatform.events.commands.ReleaseInventoryCommand;
import com.orderplatform.events.commands.ReserveInventoryCommand;
import com.orderplatform.inventory.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryCommandListener {

    private final InventoryService service;

    public InventoryCommandListener(InventoryService service) {
        this.service = service;
    }

    @KafkaListener(topics = Topics.RESERVE_INVENTORY, groupId = "inventory-service")
    public void onReserve(ReserveInventoryCommand cmd) {
        try {
            service.handleReserve(cmd);
        } catch (InventoryService.ReservationFailure failure) {
            // The reserve transaction rolled back; now report failure so the saga compensates.
            service.publishReservationFailed(failure.command, failure.reason);
        }
    }

    @KafkaListener(topics = Topics.RELEASE_INVENTORY, groupId = "inventory-service")
    public void onRelease(ReleaseInventoryCommand cmd) {
        service.handleRelease(cmd);
    }
}
