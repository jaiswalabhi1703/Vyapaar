package com.orderplatform.payment.service;

import com.orderplatform.events.Topics;
import com.orderplatform.events.commands.AuthorizePaymentCommand;
import com.orderplatform.events.commands.RefundPaymentCommand;
import com.orderplatform.events.events.PaymentAuthorizedEvent;
import com.orderplatform.events.events.PaymentFailedEvent;
import com.orderplatform.payment.domain.Payment;
import com.orderplatform.payment.domain.PaymentStatus;
import com.orderplatform.payment.payu.PayuClient;
import com.orderplatform.payment.payu.PayuProperties;
import com.orderplatform.payment.repo.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives payments through PayU's hosted checkout. The saga's AuthorizePaymentCommand no longer
 * authorizes synchronously — it creates a PENDING payment with a PayU transaction id. The customer
 * is redirected to PayU by the frontend; when PayU calls back (see PayuCallbackController) we verify
 * the response hash and only then emit PaymentAuthorized / PaymentFailed to resume the saga.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final PayuClient payu;
    private final PayuProperties props;
    private final KafkaTemplate<String, Object> kafka;

    public PaymentService(PaymentRepository payments, PayuClient payu, PayuProperties props,
                          KafkaTemplate<String, Object> kafka) {
        this.payments = payments;
        this.payu = payu;
        this.props = props;
        this.kafka = kafka;
    }

    /**
     * Handle the saga command: create (or reuse) a PENDING PayU payment. Idempotent on
     * idempotencyKey (the sagaId): a redelivered command replays the existing outcome instead of
     * creating a second checkout or charging twice.
     */
    @Transactional
    public void handleAuthorize(AuthorizePaymentCommand cmd) {
        Optional<Payment> existing = payments.findByIdempotencyKey(cmd.idempotencyKey());
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (p.getStatus() == PaymentStatus.PENDING) {
                log.info("[saga {}] AuthorizePaymentCommand redelivered; checkout already pending (txnid {})",
                        cmd.sagaId(), p.getTxnid());
            } else {
                log.info("[saga {}] duplicate AuthorizePaymentCommand -> replaying result {}",
                        cmd.sagaId(), p.getStatus());
                emitForStatus(cmd.sagaId(), p);
            }
            return;
        }

        String txnid = "o" + cmd.orderId() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Payment payment = payments.save(
                Payment.pending(cmd.orderId(), cmd.amount(), cmd.idempotencyKey(), txnid));
        log.info("[saga {}] created PENDING PayU payment {} (txnid {}) for order {} — awaiting customer",
                cmd.sagaId(), payment.getId(), txnid, cmd.orderId());
    }

    /** Build the PayU checkout form for a PENDING payment, or empty if it isn't awaiting payment. */
    @Transactional(readOnly = true)
    public Optional<PayuCheckout> checkoutFor(Long orderId) {
        return payments.findByOrderId(orderId)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .map(p -> {
                    String sagaId = p.getIdempotencyKey();
                    String productinfo = "Order #" + p.getOrderId();
                    Map<String, String> params = payu.buildCheckoutParams(
                            p.getTxnid(), p.getAmount(), productinfo,
                            "customer", props.getDefaultEmail(), props.getDefaultPhone(),
                            sagaId, p.getOrderId());
                    return new PayuCheckout(props.getPaymentUrl(), params);
                });
    }

    /**
     * Process a PayU callback (success or failure). Verifies the response hash, transitions the
     * payment, and emits the saga event exactly once. Returns the orderId for the browser redirect.
     */
    @Transactional
    public CallbackResult confirmFromCallback(Map<String, String> p) {
        if (!payu.verifyResponse(p)) {
            log.warn("PayU callback hash verification FAILED for txnid {} — ignoring", p.get("txnid"));
            return new CallbackResult(null, false);
        }
        String txnid = p.get("txnid");
        Payment payment = payments.findByTxnid(txnid).orElse(null);
        if (payment == null) {
            log.warn("PayU callback for unknown txnid {}", txnid);
            return new CallbackResult(null, false);
        }
        String sagaId = payment.getIdempotencyKey();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            // Already resolved (double callback / refresh): don't re-emit, just report current state.
            log.info("[saga {}] PayU callback for already-{} txnid {}", sagaId, payment.getStatus(), txnid);
            return new CallbackResult(payment.getOrderId(), payment.getStatus() == PaymentStatus.AUTHORIZED);
        }

        boolean success = "success".equalsIgnoreCase(p.getOrDefault("status", ""));
        String mihpayid = p.get("mihpayid");
        if (success) {
            payment.markAuthorized(mihpayid);
        } else {
            payment.markDeclined(mihpayid);
        }
        payments.save(payment);
        emitForStatus(sagaId, payment);
        log.info("[saga {}] PayU callback resolved txnid {} -> {} (mihpayid {})",
                sagaId, txnid, payment.getStatus(), mihpayid);
        return new CallbackResult(payment.getOrderId(), success);
    }

    private void emitForStatus(String sagaId, Payment payment) {
        if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
            kafka.send(Topics.PAYMENT_EVENTS, sagaId,
                    new PaymentAuthorizedEvent(sagaId, payment.getOrderId(), String.valueOf(payment.getId())));
        } else if (payment.getStatus() == PaymentStatus.DECLINED) {
            kafka.send(Topics.PAYMENT_EVENTS, sagaId,
                    new PaymentFailedEvent(sagaId, payment.getOrderId(), "payment " + payment.getStatus()));
        }
    }

    /** Compensation: refund the authorized payment for this order. Idempotent. */
    @Transactional
    public void handleRefund(RefundPaymentCommand cmd) {
        payments.findByOrderId(cmd.orderId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                // NOTE: a production refund would call PayU's refund API with the mihpayid; for the
                // demo we mark it refunded locally so the saga's compensation path completes.
                payment.markRefunded();
                payments.save(payment);
                log.info("[saga {}] refunded payment {} (mihpayid {}) for order {}",
                        cmd.sagaId(), payment.getId(), payment.getMihpayid(), cmd.orderId());
            }
        });
    }

    /** The PayU action URL plus the form fields the browser must POST to it. */
    public record PayuCheckout(String action, Map<String, String> params) {
    }

    /** Outcome of a callback: which order, and whether payment succeeded. */
    public record CallbackResult(Long orderId, boolean success) {
    }
}
