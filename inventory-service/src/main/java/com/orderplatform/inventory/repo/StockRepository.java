package com.orderplatform.inventory.repo;

import com.orderplatform.inventory.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {
}
