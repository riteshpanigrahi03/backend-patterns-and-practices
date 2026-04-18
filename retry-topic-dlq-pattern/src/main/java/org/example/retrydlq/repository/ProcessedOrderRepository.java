package org.example.retrydlq.repository;

import org.example.retrydlq.domain.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, Long> {

    boolean existsByEventId(String eventId);
}
