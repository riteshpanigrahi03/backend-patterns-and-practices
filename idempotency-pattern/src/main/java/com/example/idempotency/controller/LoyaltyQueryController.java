package com.example.idempotency.controller;

import com.example.idempotency.domain.ProcessedEvent;
import com.example.idempotency.domain.RewardLedgerEntry;
import com.example.idempotency.repository.ProcessedEventRepository;
import com.example.idempotency.repository.RewardLedgerEntryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyQueryController {

    private final RewardLedgerEntryRepository rewardLedgerEntryRepository;
    private final ProcessedEventRepository processedEventRepository;

    public LoyaltyQueryController(
            RewardLedgerEntryRepository rewardLedgerEntryRepository,
            ProcessedEventRepository processedEventRepository) {
        this.rewardLedgerEntryRepository = rewardLedgerEntryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @GetMapping("/ledger")
    public List<RewardLedgerEntry> ledger(@RequestParam(required = false) String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return rewardLedgerEntryRepository.findAllByOrderByCreatedAtAsc();
        }
        return rewardLedgerEntryRepository.findByCustomerIdOrderByCreatedAtAsc(customerId);
    }

    @GetMapping("/processed-events")
    public List<ProcessedEvent> processedEvents() {
        return processedEventRepository.findAllByOrderByProcessedAtAsc();
    }
}
