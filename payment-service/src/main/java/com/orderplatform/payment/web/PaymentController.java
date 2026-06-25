package com.orderplatform.payment.web;

import com.orderplatform.payment.domain.Payment;
import com.orderplatform.payment.repo.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only window into payments, handy for demos and debugging. */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository payments;

    public PaymentController(PaymentRepository payments) {
        this.payments = payments;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Payment> byOrder(@PathVariable Long orderId) {
        return payments.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
