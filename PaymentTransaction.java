package net.codejava.payment.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.codejava.payment.dto.AmountBreakdown;

/**
 * One attempt to pay for an order. Records the amounts, the method used,
 * every status change, any refunds, and a copy of the items bought.
 * (Named PaymentTransaction because TRANSACTION is a reserved SQL word.)
 */
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-friendly reference shown to the customer, e.g. SPM-20261008-7K3QXA. */
    @Column(nullable = false, unique = true)
    private String reference;

    /**
     * Unique id sent by the checkout page for each payment attempt.
     * If the same id arrives twice (double click, page refresh), the
     * second request gets the first result instead of paying again.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "method_label", nullable = false)
    private String methodLabel;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal surcharge;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "refunded_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TransactionEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Refund> refunds = new ArrayList<>();

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String reference, String idempotencyKey, String customerId,
                              PaymentMethod paymentMethod, AmountBreakdown amount) {
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.methodLabel = paymentMethod.getDisplayLabel();
        this.subtotal = amount.subtotal();
        this.deliveryFee = amount.deliveryFee();
        this.surcharge = amount.surcharge();
        this.totalAmount = amount.total();
        this.createdAt = LocalDateTime.now();
        changeStatus(TransactionStatus.PENDING, "Payment of FJ$" + totalAmount + " created.");
    }

    /** Moves to a new status and records it in the history. */
    public void changeStatus(TransactionStatus newStatus, String message) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        events.add(new TransactionEvent(this, newStatus, message));
    }

    public void markSucceeded(String gatewayReference, String message) {
        this.gatewayReference = gatewayReference;
        changeStatus(TransactionStatus.SUCCESS, message);
    }

    public void markFailed(String reason) {
        this.failureReason = reason;
        changeStatus(TransactionStatus.FAILED, reason);
    }

    public void addLine(TransactionLine line) {
        line.setTransaction(this);
        lines.add(line);
    }

    /** Records a refund and updates the status to REFUNDED or PARTIALLY_REFUNDED. */
    public void addRefund(Refund refund) {
        refund.setTransaction(this);
        refunds.add(refund);
        refundedAmount = refundedAmount.add(refund.getAmount());
        TransactionStatus newStatus = getRefundableAmount().signum() == 0
                ? TransactionStatus.REFUNDED
                : TransactionStatus.PARTIALLY_REFUNDED;
        changeStatus(newStatus, "Refunded FJ$" + refund.getAmount() + " (" + refund.getReason() + ").");
    }

    /** @return true if money was taken and not yet fully refunded */
    public boolean canBeRefunded() {
        return (status == TransactionStatus.SUCCESS || status == TransactionStatus.PARTIALLY_REFUNDED)
                && getRefundableAmount().signum() > 0;
    }

    public BigDecimal getRefundableAmount() {
        return totalAmount.subtract(refundedAmount);
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getMethodLabel() {
        return methodLabel;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<TransactionLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public List<TransactionEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<Refund> getRefunds() {
        return Collections.unmodifiableList(refunds);
    }
}
