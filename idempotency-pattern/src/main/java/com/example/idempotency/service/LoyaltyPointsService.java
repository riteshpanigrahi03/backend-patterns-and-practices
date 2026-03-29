package com.example.idempotency.service;

import com.example.idempotency.domain.ProcessedEvent;
import com.example.idempotency.domain.RewardLedgerEntry;
import com.example.idempotency.dto.OrderPlacedEvent;
import com.example.idempotency.repository.ProcessedEventRepository;
import com.example.idempotency.repository.RewardLedgerEntryRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LoyaltyPointsService {

    static final String CONSUMER_NAME = "loyalty-points-service";
    private static final Logger log = LoggerFactory.getLogger(LoyaltyPointsService.class);

    private final RewardLedgerEntryRepository rewardLedgerEntryRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Value("${app.loyalty.idempotency-enabled:false}")
    private boolean idempotencyEnabled;

    public LoyaltyPointsService(
            RewardLedgerEntryRepository rewardLedgerEntryRepository,
            ProcessedEventRepository processedEventRepository) {
        this.rewardLedgerEntryRepository = rewardLedgerEntryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    public void process(OrderPlacedEvent event) {
        if (!idempotencyEnabled) {
            processWithoutIdempotency(event);
            return;
        }

        try {
            processWithIdempotency(event);
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate event {} detected during transaction commit. Reward entry rolled back.", event.eventId());
        }
    }

    @Transactional
    public void processWithoutIdempotency(OrderPlacedEvent event) {
        rewardLedgerEntryRepository.save(toRewardEntry(event));
        log.info("Processed order {} without idempotency. Added {} points for customer {}.",
                event.orderId(), event.rewardPoints(), event.customerId());
    }

    @Transactional
    public void processWithIdempotency(OrderPlacedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Skipping duplicate event {} because it already exists in processed_events.", event.eventId());
            return;
        }

        rewardLedgerEntryRepository.save(toRewardEntry(event));
        processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId(), CONSUMER_NAME, Instant.now()));

        log.info("Processed order {} with idempotency. Added {} points for customer {}.",
                event.orderId(), event.rewardPoints(), event.customerId());
    }

    private RewardLedgerEntry toRewardEntry(OrderPlacedEvent event) {
        return new RewardLedgerEntry(
                event.eventId(),
                event.orderId(),
                event.customerId(),
                event.rewardPoints(),
                Instant.now());
    }
}
