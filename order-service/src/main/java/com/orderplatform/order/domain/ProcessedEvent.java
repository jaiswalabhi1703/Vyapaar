package com.orderplatform.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Idempotency ledger for inbound events (sagaId + event type), so a redelivered event
 *  never advances the saga twice. */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "event_key")
    private String eventKey;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventKey) {
        this.eventKey = eventKey;
        this.processedAt = Instant.now();
    }

    public String getEventKey() {
        return eventKey;
    }
}
