package com.orderplatform.order.saga;

import com.orderplatform.order.domain.OutboxEvent;
import com.orderplatform.order.repo.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Polls the outbox and publishes unsent rows to Kafka. The {@code __TypeId__} header lets
 * the consuming services' JsonDeserializer pick the right class for the JSON payload.
 * If a publish fails the row stays unpublished and is retried on the next tick — at-least-once
 * delivery, which the consumers dedupe.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TYPE_HEADER = "__TypeId__";

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outbox.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    event.getTopic(), event.getMessageKey(), event.getPayload());
            record.headers().add(new RecordHeader(TYPE_HEADER,
                    event.getTypeId().getBytes(StandardCharsets.UTF_8)));
            try {
                kafka.send(record).get();
                event.markPublished();
                log.debug("relayed outbox {} -> {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.warn("outbox relay failed for {} (will retry): {}", event.getId(), e.getMessage());
                // leave unpublished; next tick retries
                return;
            }
        }
    }
}
