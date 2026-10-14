package net.codejava.order.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.codejava.common.exception.ValidationException;

/**
 * A customer's order (Module 6). Named CustomerOrder because ORDER is a
 * reserved SQL word.
 */
@Entity
@Table(name = "customer_order")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "payment_reference", nullable = false, unique = true)
    private String paymentReference;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal surcharge;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "refunded_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "receipt_number", unique = true)
    private String receiptNumber;

    @Column(name = "receipt_issued_at")
    private LocalDateTime receiptIssuedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    /** Order Creation. */
    public CustomerOrder(String orderNumber, String customerId, String paymentReference, String paymentMethod,
                         BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal surcharge, BigDecimal total) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.paymentReference = paymentReference;
        this.paymentMethod = paymentMethod;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.surcharge = surcharge;
        this.total = total;
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    /** Order Confirmation: only a new order with at least one item can be confirmed. */
    public void confirm(LocalDateTime now) {
        if (status != OrderStatus.CREATED) {
            throw new ValidationException("Only a new order can be confirmed.");
        }
        if (items.isEmpty()) {
            throw new ValidationException("An order needs at least one item.");
        }
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    /** Receipt Creation: a receipt can only be issued once, for a confirmed order. */
    public void issueReceipt(String receiptNumber, LocalDateTime now) {
        if (this.receiptNumber != null) {
            throw new ValidationException("A receipt has already been issued for this order.");
        }
        if (status == OrderStatus.CREATED) {
            throw new ValidationException("Confirm the order before issuing a receipt.");
        }
        this.receiptNumber = receiptNumber;
        this.receiptIssuedAt = now;
    }

    public void markDeliveryScheduled() {
        requireStatus(OrderStatus.CONFIRMED, "Only confirmed orders can be booked for delivery.");
        this.status = OrderStatus.DELIVERY_SCHEDULED;
    }

    /** The delivery booking was cancelled, so the order can be booked again. */
    public void markDeliveryCancelled() {
        if (status == OrderStatus.DELIVERY_SCHEDULED) {
            this.status = OrderStatus.CONFIRMED;
        }
    }

    public void markDelivered() {
        requireStatus(OrderStatus.DELIVERY_SCHEDULED, "Only orders booked for delivery can be delivered.");
        this.status = OrderStatus.DELIVERED;
    }

    public void recordRefund(BigDecimal totalRefunded, boolean fullyRefunded) {
        this.refundedAmount = totalRefunded;
        if (fullyRefunded) {
            this.status = OrderStatus.REFUNDED;
        }
    }

    private void requireStatus(OrderStatus expected, String message) {
        if (status != expected) {
            throw new ValidationException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
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

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public LocalDateTime getReceiptIssuedAt() {
        return receiptIssuedAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
