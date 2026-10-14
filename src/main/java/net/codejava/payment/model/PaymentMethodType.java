package net.codejava.payment.model;

/**
 * Payment methods the Bula Maleka supermarket accepts.
 */
public enum PaymentMethodType {

    CARD("Visa / Mastercard", true),
    MPAISA("M-PAiSA", false),
    MYCASH("MyCash", false);

    private final String displayName;
    private final boolean card;

    PaymentMethodType(String displayName, boolean card) {
        this.displayName = displayName;
        this.card = card;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** @return true for bank cards, false for mobile wallets */
    public boolean isCard() {
        return card;
    }
}
