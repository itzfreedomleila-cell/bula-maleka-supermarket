package net.codejava.payment.dto;

import net.codejava.payment.model.PaymentMethodType;

/**
 * JSON body for registering a payment method.
 * Card: type=CARD, holderName, cardNumber, expiry ("MM/YY").
 * Wallet: type=MPAISA or MYCASH, holderName, mobileNumber (7 digits).
 */
public record RegisterPaymentMethodRequest(
        PaymentMethodType type,
        String holderName,
        String cardNumber,
        String expiry,
        String mobileNumber) {
}
