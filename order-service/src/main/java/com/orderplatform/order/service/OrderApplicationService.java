package com.orderplatform.order.service;

import com.orderplatform.order.domain.OrderEntity;
import com.orderplatform.order.domain.OrderItem;
import com.orderplatform.order.repo.OrderRepository;
import com.orderplatform.order.repo.SagaStateRepository;
import com.orderplatform.order.saga.SagaOrchestrator;
import com.orderplatform.order.web.dto.OrderResponse;
import com.orderplatform.order.web.dto.PlaceOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderApplicationService {

    private final OrderRepository orders;
    private final SagaStateRepository sagas;
    private final SagaOrchestrator orchestrator;

    public OrderApplicationService(OrderRepository orders, SagaStateRepository sagas,
                                   SagaOrchestrator orchestrator) {
        this.orders = orders;
        this.sagas = sagas;
        this.orchestrator = orchestrator;
    }

    /**
     * Persist the order and start the saga in one transaction. The first command lands in the
     * outbox (same tx), so by commit time either both the order exists and the command is
     * queued, or neither. Returns immediately — the caller gets 202 Accepted.
     */
    @Transactional
    public OrderResponse placeOrder(String userId, PlaceOrderRequest req) {
        List<OrderItem> items = req.items().stream()
                .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList();
        OrderEntity order = orders.save(new OrderEntity(userId, items, req.shippingAddress()));
        orchestrator.start(order);
        return OrderResponse.of(order, sagas.findById("saga-" + order.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId) {
        OrderEntity order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No order " + orderId));
        return OrderResponse.of(order, sagas.findById("saga-" + orderId).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> historyFor(String userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(o -> OrderResponse.of(o, sagas.findById("saga-" + o.getId()).orElse(null)))
                .toList();
    }
}
