package net.codejava.payment.dto;

/**
 * JSON body for paying for the current cart.
 *
 * @param paymentMethodId id of a registered payment method
 * @param cvv             card security code (cards only; checked, never stored)
 * @param idempotencyKey  unique id per payment attempt, used to stop double payments
 */
public record PaymentRequest(Long paymentMethodId, String cvv, String idempotencyKey) {
}
