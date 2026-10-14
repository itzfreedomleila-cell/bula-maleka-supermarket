package net.codejava.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A payment method registered by a customer (Payment Registration).
 * <p>
 * Security: the full card number and the CVV are NEVER stored. Only the
 * brand, the last 4 digits and the expiry date are kept, which is enough
 * to show the card to the customer.
 */
@Entity
@Table(name = "payment_method")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType type;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    /** Card only: VISA or MASTERCARD. */
    private String brand;

    /** Card only: last 4 digits. */
    @Column(name = "last_four")
    private String lastFour;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    /** Wallet only: 7-digit Fiji mobile number linked to the wallet. */
    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Required by JPA. */
    protected PaymentMethod() {
    }

    private PaymentMethod(String customerId, PaymentMethodType type, String holderName) {
        this.customerId = customerId;
        this.type = type;
        this.holderName = holderName;
        this.createdAt = LocalDateTime.now();
    }

    /** Creates a card payment method from already-validated details. */
    public static PaymentMethod card(String customerId, String holderName, String brand,
                                     String lastFour, int expiryMonth, int expiryYear) {
        PaymentMethod method = new PaymentMethod(customerId, PaymentMethodType.CARD, holderName);
        method.brand = brand;
        method.lastFour = lastFour;
        method.expiryMonth = expiryMonth;
        method.expiryYear = expiryYear;
        return method;
    }

    /** Creates a mobile wallet payment method (M-PAiSA or MyCash). */
    public static PaymentMethod wallet(String customerId, PaymentMethodType type,
                                       String holderName, String mobileNumber) {
        PaymentMethod method = new PaymentMethod(customerId, type, holderName);
        method.mobileNumber = mobileNumber;
        return method;
    }

    /**
     * @return a safe label for screens and receipts,
     *         e.g. "VISA ending 4242" or "M-PAiSA ending 3456"
     */
    public String getDisplayLabel() {
        if (type.isCard()) {
            return brand + " ending " + lastFour;
        }
        return type.getDisplayName() + " ending " + mobileNumber.substring(mobileNumber.length() - 4);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    /** Used by unit tests only. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentMethodType getType() {
        return type;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getBrand() {
        return brand;
    }

    public String getLastFour() {
        return lastFour;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
