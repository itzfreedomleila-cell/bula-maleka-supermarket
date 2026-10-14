package net.codejava.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;
import net.codejava.payment.config.PaymentSettings;
import net.codejava.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * A pretend payment provider for development and demos. NO REAL MONEY MOVES.
 * <p>
 * Test rules so every outcome can be demonstrated:
 * <ul>
 *   <li>Card ending 0002 (e.g. 4000 0000 0000 0002): declined by the bank.</li>
 *   <li>Wallet mobile number ending 0000: insufficient wallet balance.</li>
 *   <li>Wallet payment above payment.wallet-limit: over the wallet limit.</li>
 *   <li>Everything else: approved.</li>
 * </ul>
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final PaymentSettings settings;

    public SimulatedPaymentGateway(PaymentSettings settings) {
        this.settings = settings;
    }

    @Override
    public GatewayResult charge(PaymentMethod method, BigDecimal amount, String reference) {
        if (method.getType().isCard()) {
            if ("0002".equals(method.getLastFour())) {
                return GatewayResult.declined("Card declined by the bank. Try another payment method.");
            }
        } else {
            if (method.getMobileNumber().endsWith("0000")) {
                return GatewayResult.declined("Insufficient " + method.getType().getDisplayName()
                        + " balance. Top up your wallet or choose another method.");
            }
            if (amount.compareTo(settings.getWalletLimit()) > 0) {
                return GatewayResult.declined("Amount is over the FJ$" + settings.getWalletLimit()
                        + " wallet limit. Pay by card or split the order.");
            }
        }
        return GatewayResult.approved("SIM-" + shortId());
    }

    @Override
    public GatewayResult refund(String gatewayReference, BigDecimal amount) {
        if (gatewayReference == null) {
            return GatewayResult.declined("Original payment reference is missing.");
        }
        return GatewayResult.approved("SIMR-" + shortId());
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
