package com.orderplatform.order.web;

import com.orderplatform.order.service.OrderApplicationService;
import com.orderplatform.order.web.dto.OrderResponse;
import com.orderplatform.order.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderApplicationService service;

    public OrderController(OrderApplicationService service) {
        this.service = service;
    }

    /**
     * Place an order. Returns 202 Accepted: the order is accepted, not yet completed — the saga
     * runs asynchronously. Poll GET /orders/{id} to watch it progress. The user identity is
     * forwarded by the gateway in X-User-Id (defaults to "demo-user" for direct calls).
     */
    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") String userId,
            @Valid @RequestBody PlaceOrderRequest req) {
        OrderResponse created = service.placeOrder(userId, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create("/orders/" + created.id()))
                .body(created);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public List<OrderResponse> history(
            @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") String userId) {
        return service.historyFor(userId);
    }
}
