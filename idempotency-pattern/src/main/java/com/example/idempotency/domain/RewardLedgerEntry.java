package com.example.idempotency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reward_ledger")
public class RewardLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Column(nullable = false, length = 100)
    private String customerId;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private Instant createdAt;

    protected RewardLedgerEntry() {
    }

    public RewardLedgerEntry(String eventId, String orderId, String customerId, int points, Instant createdAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.points = points;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getPoints() {
        return points;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
