package com.orderplatform.order.saga;

import com.orderplatform.order.domain.SagaState;
import com.orderplatform.order.domain.SagaStatus;
import com.orderplatform.order.repo.SagaStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Per-step timeout. If a saga sits in a non-terminal state longer than {@code saga.step-timeout}
 * (a service crashed, a message was lost), it is force-compensated so it can't hang forever.
 * This is also the crash-recovery net: on restart the persisted saga_state is still here, and a
 * saga stuck awaiting a reply gets unwound once its deadline passes.
 */
@Component
public class TimeoutScanner {

    private static final Logger log = LoggerFactory.getLogger(TimeoutScanner.class);
    private static final List<SagaStatus> TERMINAL_OR_BUSY =
            List.of(SagaStatus.COMPLETED, SagaStatus.COMPENSATED, SagaStatus.COMPENSATING);

    private final SagaStateRepository sagas;
    private final SagaOrchestrator orchestrator;
    private final Duration stepTimeout;

    public TimeoutScanner(SagaStateRepository sagas, SagaOrchestrator orchestrator,
                          @Value("${saga.step-timeout:30s}") Duration stepTimeout) {
        this.sagas = sagas;
        this.orchestrator = orchestrator;
        this.stepTimeout = stepTimeout;
    }

    @Scheduled(fixedDelayString = "${saga.timeout-scan-interval-ms:5000}")
    public void scan() {
        Instant cutoff = Instant.now().minus(stepTimeout);
        List<SagaState> stuck = sagas.findByStatusNotInAndUpdatedAtBefore(TERMINAL_OR_BUSY, cutoff);
        for (SagaState saga : stuck) {
            log.warn("[saga {}] timed out awaiting {} — forcing compensation",
                    saga.getSagaId(), saga.getCurrentStep());
            orchestrator.compensate(saga.getSagaId(), saga.getOrderId(),
                    "TIMEOUT:" + saga.getCurrentStep(), "step timed out");
        }
    }
}
