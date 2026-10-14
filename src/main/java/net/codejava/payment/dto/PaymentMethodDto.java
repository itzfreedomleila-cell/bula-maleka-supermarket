package net.codejava.payment.dto;

import net.codejava.payment.model.PaymentMethod;
import net.codejava.payment.model.PaymentMethodType;

/** A registered payment method as shown to the customer (no sensitive data). */
public record PaymentMethodDto(
        Long id,
        PaymentMethodType type,
        String typeName,
        String label,
        String holderName,
        String expiry,
        boolean requiresCvv) {

    public static PaymentMethodDto from(PaymentMethod m) {
        String expiry = m.getType().isCard()
                ? String.format("%02d/%02d", m.getExpiryMonth(), m.getExpiryYear() % 100)
                : null;
        return new PaymentMethodDto(m.getId(), m.getType(), m.getType().getDisplayName(),
                m.getDisplayLabel(), m.getHolderName(), expiry, m.getType().isCard());
    }
}
