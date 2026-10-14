package net.codejava.payment.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import net.codejava.payment.dto.RegisterPaymentMethodRequest;
import net.codejava.payment.exception.PaymentValidationException;
import net.codejava.payment.model.PaymentMethod;
import net.codejava.payment.model.PaymentMethodType;
import org.junit.jupiter.api.Test;

/**
 * Tests for Payment Validation. "Today" is fixed to 8 October 2026 so the
 * expiry tests give the same result whenever they run.
 */
class PaymentValidatorTest {

    private final PaymentValidator validator = new PaymentValidator(
            Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneId.of("Pacific/Fiji")));

    private RegisterPaymentMethodRequest card(String number, String expiry) {
        return new RegisterPaymentMethodRequest(PaymentMethodType.CARD, "Camari K", number, expiry, null);
    }

    @Test
    void luhn_acceptsValidAndRejectsMistypedNumbers() {
        assertTrue(PaymentValidator.passesLuhn("4242424242424242"));
        assertFalse(PaymentValidator.passesLuhn("4242424242424241"));
    }

    @Test
    void detectBrand_findsVisaAndMastercard() {
        assertEquals("VISA", PaymentValidator.detectBrand("4242424242424242"));
        assertEquals("MASTERCARD", PaymentValidator.detectBrand("5555555555554444"));
        assertNull(PaymentValidator.detectBrand("378282246310005")); // Amex not accepted
    }

    @Test
    void validCard_isAccepted() {
        assertDoesNotThrow(() -> validator.validateRegistration(card("4242 4242 4242 4242", "08/28")));
    }

    @Test
    void expiredCard_isRejected() {
        assertThrows(PaymentValidationException.class,
                () -> validator.validateRegistration(card("4242424242424242", "09/26")));
    }

    @Test
    void badCardNumber_isRejected() {
        assertThrows(PaymentValidationException.class,
                () -> validator.validateRegistration(card("1234 5678 9012 3456", "08/28")));
    }

    @Test
    void wallet_needsSevenDigitMobileNumber() {
        assertDoesNotThrow(() -> validator.validateRegistration(new RegisterPaymentMethodRequest(
                PaymentMethodType.MPAISA, "Camari K", null, null, "912 3456")));
        assertThrows(PaymentValidationException.class, () -> validator.validateRegistration(
                new RegisterPaymentMethodRequest(PaymentMethodType.MYCASH, "Camari K", null, null, "12345")));
    }

    @Test
    void cardPayment_needsThreeDigitCvv() {
        PaymentMethod visa = PaymentMethod.card("c1", "Camari K", "VISA", "4242", 8, 2028);
        assertDoesNotThrow(() -> validator.validateForPayment(visa, "123", new BigDecimal("30.00")));
        assertThrows(PaymentValidationException.class,
                () -> validator.validateForPayment(visa, "12", new BigDecimal("30.00")));
    }
}
