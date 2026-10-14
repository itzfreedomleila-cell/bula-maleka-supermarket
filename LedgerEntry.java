package net.codejava.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A record of money moving between accounts (Money Transfer).
 * Payments move money from the customer to the store's settlement
 * account; refunds move it back.
 */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType type;

    @Column(name = "from_account", nullable = false)
    private String fromAccount;

    @Column(name = "to_account", nullable = false)
    private String toAccount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(String transactionReference, LedgerEntryType type,
                       String fromAccount, String toAccount, BigDecimal amount) {
        this.transactionReference = transactionReference;
        this.type = type;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public LedgerEntryType getType() {
        return type;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
