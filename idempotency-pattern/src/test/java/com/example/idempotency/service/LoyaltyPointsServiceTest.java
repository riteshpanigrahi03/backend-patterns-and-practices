package com.example.idempotency.service;

import com.example.idempotency.dto.OrderPlacedEvent;
import com.example.idempotency.repository.ProcessedEventRepository;
import com.example.idempotency.repository.RewardLedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class LoyaltyPointsServiceTest {

    @Autowired
    private LoyaltyPointsService loyaltyPointsService;

    @Autowired
    private RewardLedgerEntryRepository rewardLedgerEntryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private OrderPlacedEvent event;

    @BeforeEach
    void setUp() {
        rewardLedgerEntryRepository.deleteAll();
        processedEventRepository.deleteAll();
        event = new OrderPlacedEvent(
                "evt-123",
                "order-123",
                "customer-123",
                new BigDecimal("120.00"),
                12,
                Instant.now());
    }

    @Test
    void processWithoutIdempotency_shouldInsertRewardEntryEveryTime() {
        loyaltyPointsService.processWithoutIdempotency(event);
        loyaltyPointsService.processWithoutIdempotency(event);

        assertThat(rewardLedgerEntryRepository.findAll()).hasSize(2);
        assertThat(processedEventRepository.findAll()).isEmpty();
    }

    @Test
    void processWithIdempotency_shouldInsertRewardEntryOnlyOnce() {
        loyaltyPointsService.processWithIdempotency(event);
        loyaltyPointsService.processWithIdempotency(event);

        assertThat(rewardLedgerEntryRepository.findAll()).hasSize(1);
        assertThat(processedEventRepository.findAll()).hasSize(1);
        assertThat(processedEventRepository.findAll().get(0).getEventId()).isEqualTo("evt-123");
    }
}
