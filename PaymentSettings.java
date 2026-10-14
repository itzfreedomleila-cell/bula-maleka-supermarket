package net.codejava.payment.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Payment business settings, read from application.properties so nothing
 * is hard-coded. The values after ":" are defaults used if a property is missing.
 */
@Component
public class PaymentSettings {

    private final BigDecimal deliveryFee;
    private final BigDecimal freeDeliveryThreshold;
    private final BigDecimal cardSurchargeRate;
    private final BigDecimal walletLimit;
    private final String storeAccount;

    public PaymentSettings(
            @Value("${payment.delivery-fee:7.50}") BigDecimal deliveryFee,
            @Value("${payment.free-delivery-threshold:100.00}") BigDecimal freeDeliveryThreshold,
            @Value("${payment.card-surcharge-rate:0.015}") BigDecimal cardSurchargeRate,
            @Value("${payment.wallet-limit:1000.00}") BigDecimal walletLimit,
            @Value("${payment.store-account:SUPERMARKET-SETTLEMENT}") String storeAccount) {
        this.deliveryFee = deliveryFee.setScale(2, RoundingMode.HALF_UP);
        this.freeDeliveryThreshold = freeDeliveryThreshold.setScale(2, RoundingMode.HALF_UP);
        this.cardSurchargeRate = cardSurchargeRate;
        this.walletLimit = walletLimit.setScale(2, RoundingMode.HALF_UP);
        this.storeAccount = storeAccount;
    }

    /** Delivery charge for orders below the free-delivery threshold. */
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    /** Orders at or above this subtotal get free delivery. */
    public BigDecimal getFreeDeliveryThreshold() {
        return freeDeliveryThreshold;
    }

    /** Extra charge for card payments, e.g. 0.015 = 1.5%. */
    public BigDecimal getCardSurchargeRate() {
        return cardSurchargeRate;
    }

    /** Largest single payment allowed from a mobile wallet. */
    public BigDecimal getWalletLimit() {
        return walletLimit;
    }

    /** The store's account that receives payments. */
    public String getStoreAccount() {
        return storeAccount;
    }
}
