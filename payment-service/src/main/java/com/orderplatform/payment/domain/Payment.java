package com.orderplatform.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** Unique business key: a redelivered authorize for the same key returns the same result. */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    /** Our PayU transaction id (sent as {@code txnid}); how a callback finds its payment row. */
    @Column(name = "txnid", unique = true)
    private String txnid;

    /** PayU's own payment id ({@code mihpayid}), captured from the callback once paid. */
    @Column(name = "mihpayid")
    private String mihpayid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    /** Legacy/direct constructor (status known up front). */
    public Payment(Long orderId, BigDecimal amount, PaymentStatus status, String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    /** Create a PENDING payment for a freshly minted PayU checkout. */
    public static Payment pending(Long orderId, BigDecimal amount, String idempotencyKey, String txnid) {
        Payment p = new Payment(orderId, amount, PaymentStatus.PENDING, idempotencyKey);
        p.txnid = txnid;
        return p;
    }

    public void markAuthorized(String mihpayid) {
        this.status = PaymentStatus.AUTHORIZED;
        this.mihpayid = mihpayid;
    }

    public void markDeclined(String mihpayid) {
        this.status = PaymentStatus.DECLINED;
        this.mihpayid = mihpayid;
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getTxnid() {
        return txnid;
    }

    public String getMihpayid() {
        return mihpayid;
    }
}
