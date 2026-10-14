package net.codejava.payment.gateway;

import java.math.BigDecimal;
import net.codejava.payment.model.PaymentMethod;

/**
 * Connection to a payment provider (bank or mobile wallet).
 * <p>
 * The system only depends on this interface. The coursework uses
 * {@link SimulatedPaymentGateway}; a real provider could be added later
 * by writing another implementation, without changing the service.
 */
public interface PaymentGateway {

    /** Takes money from the customer's payment method. */
    GatewayResult charge(PaymentMethod method, BigDecimal amount, String reference);

    /** Returns money for an earlier approved charge. */
    GatewayResult refund(String gatewayReference, BigDecimal amount);
}
