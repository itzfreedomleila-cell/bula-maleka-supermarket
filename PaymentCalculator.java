package net.codejava.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.codejava.payment.config.PaymentSettings;
import net.codejava.payment.dto.AmountBreakdown;
import net.codejava.payment.model.PaymentMethodType;
import org.springframework.stereotype.Component;

/**
 * Works out how much the customer pays (Payment Amount Calculation):
 * <pre>
 *   delivery  = 0 if subtotal >= free-delivery threshold, otherwise the delivery fee
 *   surcharge = (subtotal + delivery) x card surcharge rate   (cards only)
 *   total     = subtotal + delivery + surcharge
 * </pre>
 */
@Component
public class PaymentCalculator {

    private final PaymentSettings settings;

    public PaymentCalculator(PaymentSettings settings) {
        this.settings = settings;
    }

    public AmountBreakdown calculate(BigDecimal subtotal, PaymentMethodType type) {
        BigDecimal cleanSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal delivery = cleanSubtotal.compareTo(settings.getFreeDeliveryThreshold()) >= 0
                ? BigDecimal.ZERO.setScale(2)
                : settings.getDeliveryFee();

        BigDecimal surcharge = type != null && type.isCard()
                ? cleanSubtotal.add(delivery).multiply(settings.getCardSurchargeRate())
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal total = cleanSubtotal.add(delivery).add(surcharge);
        return new AmountBreakdown(type, cleanSubtotal, delivery, surcharge, total,
                settings.getFreeDeliveryThreshold());
    }
}
