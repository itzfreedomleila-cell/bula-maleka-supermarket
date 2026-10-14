package net.codejava.payment.gateway;

/**
 * The payment provider's answer to a charge or refund request.
 *
 * @param approved         true if the provider accepted the request
 * @param gatewayReference provider's own reference (null if declined)
 * @param message          explanation, e.g. a decline reason
 */
public record GatewayResult(boolean approved, String gatewayReference, String message) {

    public static GatewayResult approved(String gatewayReference) {
        return new GatewayResult(true, gatewayReference, "Approved");
    }

    public static GatewayResult declined(String message) {
        return new GatewayResult(false, null, message);
    }
}
