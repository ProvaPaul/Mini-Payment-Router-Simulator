package com.paymentrouter.router.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One transfer attempt between two providers: HISTORICAL data.
 * <p>
 * feePercentage, feeAmount and totalAmount are a pricing snapshot taken when the
 * transfer happened. They must never be recalculated from the provider's current fee.
 * For that reason this entity has no setters.
 */
@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transactions_transaction_id", columnNames = "transaction_id"))
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public business identifier returned by the API and sent to the DFSP. */
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    /** Provider the money is sent from. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "source_provider_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transactions_source_provider"))
    private Provider sourceProvider;

    /** Provider the money is sent to. Its fee percentage was used for pricing. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "destination_provider_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transactions_destination_provider"))
    private Provider destinationProvider;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Snapshot of the destination provider's fee percentage at transfer time. */
    @Column(name = "fee_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal feePercentage;

    /** Snapshot: fee charged for this transaction. */
    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal feeAmount;

    /** Snapshot: amount + feeAmount. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA: Hibernate creates instances through this constructor. */
    protected Transaction() {
    }

    public Transaction(
            UUID transactionId,
            Provider sourceProvider,
            Provider destinationProvider,
            BigDecimal amount,
            BigDecimal feePercentage,
            BigDecimal feeAmount,
            BigDecimal totalAmount,
            TransactionStatus status) {
        this.transactionId = transactionId;
        this.sourceProvider = sourceProvider;
        this.destinationProvider = destinationProvider;
        this.amount = amount;
        this.feePercentage = feePercentage;
        this.feeAmount = feeAmount;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Provider getSourceProvider() {
        return sourceProvider;
    }

    public Provider getDestinationProvider() {
        return destinationProvider;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
