package net.codejava.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import net.codejava.payment.config.PaymentSettings;
import net.codejava.payment.dto.AmountBreakdown;
import net.codejava.payment.model.PaymentMethodType;
import org.junit.jupiter.api.Test;

/**
 * Tests for Payment Amount Calculation.
 * Settings: delivery FJ$7.50, free delivery from FJ$100, card surcharge 1.5%.
 */
class PaymentCalculatorTest {

    private final PaymentCalculator calculator = new PaymentCalculator(new PaymentSettings(
            new BigDecimal("7.50"), new BigDecimal("100.00"), new BigDecimal("0.015"),
            new BigDecimal("1000.00"), "STORE"));

    @Test
    void smallWalletOrder_paysDeliveryButNoSurcharge() {
        AmountBreakdown a = calculator.calculate(new BigDecimal("40.00"), PaymentMethodType.MPAISA);
        assertEquals(new BigDecimal("7.50"), a.deliveryFee());
        assertEquals(new BigDecimal("0.00"), a.surcharge());
        assertEquals(new BigDecimal("47.50"), a.total());
    }

    @Test
    void smallCardOrder_paysDeliveryAndSurcharge() {
        AmountBreakdown a = calculator.calculate(new BigDecimal("40.00"), PaymentMethodType.CARD);
        assertEquals(new BigDecimal("0.71"), a.surcharge());   // 47.50 x 1.5% = 0.7125
        assertEquals(new BigDecimal("48.21"), a.total());
    }

    @Test
    void orderAtThreshold_getsFreeDelivery() {
        AmountBreakdown a = calculator.calculate(new BigDecimal("100.00"), PaymentMethodType.MYCASH);
        assertEquals(new BigDecimal("0.00"), a.deliveryFee());
        assertEquals(new BigDecimal("100.00"), a.total());
    }
}
