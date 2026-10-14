package net.codejava.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Money given back to the customer for a transaction (Refund Processing).
 */
@Entity
@Table(name = "refund")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private PaymentTransaction transaction;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String reason;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Refund() {
    }

    public Refund(BigDecimal amount, String reason, String gatewayReference) {
        this.amount = amount;
        this.reason = reason;
        this.gatewayReference = gatewayReference;
        this.createdAt = LocalDateTime.now();
    }

    void setTransaction(PaymentTransaction transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
