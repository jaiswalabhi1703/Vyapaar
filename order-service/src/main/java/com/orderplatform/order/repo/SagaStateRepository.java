package com.orderplatform.order.repo;

import com.orderplatform.order.domain.SagaState;
import com.orderplatform.order.domain.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SagaStateRepository extends JpaRepository<SagaState, String> {

    /** Sagas still in a non-terminal status that haven't advanced since {@code cutoff}. */
    List<SagaState> findByStatusNotInAndUpdatedAtBefore(List<SagaStatus> terminal, Instant cutoff);
}
