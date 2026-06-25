package com.orderplatform.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Transactional Outbox row. The orchestrator writes the outgoing command here in the SAME
 * DB transaction as the saga-state change; a relay later publishes it to Kafka and marks it
 * sent. This removes the dual-write problem (DB commits but the Kafka publish fails).
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    /** Fully-qualified type name, published as the Kafka {@code __TypeId__} header. */
    @Column(name = "type_id", nullable = false)
    private String typeId;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String messageKey, String typeId, String payload) {
        this.topic = topic;
        this.messageKey = messageKey;
        this.typeId = typeId;
        this.payload = payload;
        this.published = false;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.published = true;
    }

    public Long getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isPublished() {
        return published;
    }
}
