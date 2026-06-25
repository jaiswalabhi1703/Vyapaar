package com.orderplatform.shipping.web;

import com.orderplatform.shipping.domain.Shipment;
import com.orderplatform.shipping.repo.ShipmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only window into shipments, handy for demos and debugging. */
@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentRepository shipments;

    public ShipmentController(ShipmentRepository shipments) {
        this.shipments = shipments;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Shipment> byOrder(@PathVariable Long orderId) {
        return shipments.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
