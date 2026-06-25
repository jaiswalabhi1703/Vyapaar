package com.orderplatform.payment.kafka;

import com.orderplatform.events.Topics;
import com.orderplatform.events.commands.AuthorizePaymentCommand;
import com.orderplatform.events.commands.RefundPaymentCommand;
import com.orderplatform.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandListener {

    private final PaymentService service;

    public PaymentCommandListener(PaymentService service) {
        this.service = service;
    }

    @KafkaListener(topics = Topics.AUTHORIZE_PAYMENT, groupId = "payment-service")
    public void onAuthorize(AuthorizePaymentCommand cmd) {
        service.handleAuthorize(cmd);
    }

    @KafkaListener(topics = Topics.REFUND_PAYMENT, groupId = "payment-service")
    public void onRefund(RefundPaymentCommand cmd) {
        service.handleRefund(cmd);
    }
}
