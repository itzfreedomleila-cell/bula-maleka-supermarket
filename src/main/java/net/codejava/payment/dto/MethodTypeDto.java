package net.codejava.payment.dto;

import java.math.BigDecimal;
import net.codejava.payment.model.PaymentMethodType;

/** A payment method type the store accepts, for the selection list. */
public record MethodTypeDto(PaymentMethodType code, String displayName, boolean card, BigDecimal surchargeRate) {
}
