package com.example.idempotency.repository;

import com.example.idempotency.domain.RewardLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardLedgerEntryRepository extends JpaRepository<RewardLedgerEntry, Long> {

    List<RewardLedgerEntry> findAllByOrderByCreatedAtAsc();

    List<RewardLedgerEntry> findByCustomerIdOrderByCreatedAtAsc(String customerId);
}
