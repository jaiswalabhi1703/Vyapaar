package com.orderplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Idempotency ledger. Kafka is at-least-once, so we record every (sagaId + messageType)
 * we have already handled. The primary key's uniqueness is what guarantees a redelivered
 * command is processed exactly once (Phase 4 idempotent consumers).
 */
@Entity
@Table(name = "processed_message")
public class ProcessedMessage {

    @Id
    @Column(name = "message_key")
    private String messageKey;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedMessage() {
    }

    public ProcessedMessage(String messageKey) {
        this.messageKey = messageKey;
        this.processedAt = Instant.now();
    }

    public String getMessageKey() {
        return messageKey;
    }
}
