package com.orderplatform.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Durable saga state. The orchestrator reads/writes this on every transition so it can
 * recover after a crash and so the timeout scanner can find sagas that are stuck awaiting
 * a response.
 */
@Entity
@Table(name = "saga_state")
public class SagaState {

    @Id
    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** The command we are currently awaiting a reply to (e.g. AUTHORIZE_PAYMENT). */
    @Column(name = "current_step")
    private String currentStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    /** Furthest forward state reached; preserved even after we divert to compensation,
     *  so the UI can still show how far the happy path got. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reached_status", nullable = false)
    private SagaStatus reachedStatus;

    @Column(name = "failed_step")
    private String failedStep;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SagaState() {
    }

    public SagaState(String sagaId, Long orderId) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.status = SagaStatus.STARTED;
        this.reachedStatus = SagaStatus.STARTED;
        this.currentStep = "RESERVE_INVENTORY";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Move forward on the happy path, recording this as the furthest state reached. */
    public void advance(SagaStatus status, String nextStep) {
        this.status = status;
        this.reachedStatus = status;
        this.currentStep = nextStep;
        this.updatedAt = Instant.now();
    }

    /** Divert to compensation, remembering which step failed and why. */
    public void fail(String failedStep, String reason) {
        this.status = SagaStatus.COMPENSATING;
        this.failedStep = failedStep;
        this.failureReason = reason;
        this.currentStep = "COMPENSATING";
        this.updatedAt = Instant.now();
    }

    public void markCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.currentStep = "COMPENSATED";
        this.updatedAt = Instant.now();
    }

    public boolean isTerminal() {
        return status == SagaStatus.COMPLETED || status == SagaStatus.COMPENSATED;
    }

    public boolean isCompensating() {
        return status == SagaStatus.COMPENSATING || status == SagaStatus.COMPENSATED;
    }

    public String getSagaId() {
        return sagaId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public SagaStatus getReachedStatus() {
        return reachedStatus;
    }

    public String getFailedStep() {
        return failedStep;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
