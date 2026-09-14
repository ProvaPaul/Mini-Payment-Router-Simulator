package com.paymentrouter.router.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A DFSP known to the router, holding its CURRENT configuration.
 * <p>
 * The fee percentage here may change over time. Past transactions keep their
 * own copy of the fee values, so changing a provider never rewrites history.
 */
@Entity
@Table(
        name = "providers",
        uniqueConstraints = @UniqueConstraint(name = "uk_providers_code", columnNames = "code"))
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable business identifier used by the API, e.g. DFSP_A. */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Base URL the router uses to call this DFSP, e.g. http://dfsp-a:8081. */
    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    /** Current fee in percent, e.g. 1.50 means 1.5%. */
    @Column(name = "fee_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal feePercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProviderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA: Hibernate creates instances through this constructor. */
    protected Provider() {
    }

    public Provider(String code, String name, String baseUrl, BigDecimal feePercentage, ProviderStatus status) {
        this.code = code;
        this.name = name;
        this.baseUrl = baseUrl;
        this.feePercentage = feePercentage;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    public void setFeePercentage(BigDecimal feePercentage) {
        this.feePercentage = feePercentage;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
