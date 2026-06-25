package com.orderplatform.order.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.order.domain.OutboxEvent;
import com.orderplatform.order.repo.OutboxRepository;
import org.springframework.stereotype.Component;

/**
 * Writes outgoing commands to the outbox table. Called only from within the orchestrator's
 * transaction, so the command and the saga-state change commit atomically.
 */
@Component
public class OutboxPublisher {

    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String topic, String key, Object message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            outbox.save(new OutboxEvent(topic, key, message.getClass().getName(), payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize outbox message " + message, e);
        }
    }
}
