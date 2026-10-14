package net.codejava.payment.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.codejava.payment.dto.RegisterPaymentMethodRequest;
import net.codejava.payment.exception.PaymentValidationException;
import net.codejava.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * Checks payment details before anything is saved or charged (Payment Validation).
 */
@Component
public class PaymentValidator {

    private static final Pattern EXPIRY = Pattern.compile("^(0[1-9]|1[0-2])/(\\d{2})$");
    private static final Pattern CVV = Pattern.compile("^\\d{3}$");
    private static final Pattern FIJI_MOBILE = Pattern.compile("^\\d{7}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,59}$");

    private final Clock clock;

    /** Used by Spring: checks expiry dates against the real current date. */
    public PaymentValidator() {
        this(Clock.systemDefaultZone());
    }

    /** Used by tests to fix "today" to a known date. */
    PaymentValidator(Clock clock) {
        this.clock = clock;
    }

    /**
     * Validates a new payment method. Collects every problem so the customer
     * can fix them all at once.
     *
     * @throws PaymentValidationException if anything is wrong
     */
    public void validateRegistration(RegisterPaymentMethodRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.type() == null) {
            throw new PaymentValidationException(List.of("Choose a payment method type."));
        }
        if (request.holderName() == null || !NAME.matcher(request.holderName().trim()).matches()) {
            errors.add("Enter the name as it appears on the card or wallet (letters only).");
        }
        if (request.type().isCard()) {
            String digits = digitsOnly(request.cardNumber());
            if (digits.length() < 13 || digits.length() > 19 || !passesLuhn(digits)) {
                errors.add("Card number is not valid. Check the digits and try again.");
            } else if (detectBrand(digits) == null) {
                errors.add("Only Visa and Mastercard are accepted.");
            }
            YearMonth expiry = parseExpiry(request.expiry());
            if (expiry == null) {
                errors.add("Expiry date must be in MM/YY format, e.g. 08/28.");
            } else if (expiry.isBefore(YearMonth.now(clock))) {
                errors.add("This card has expired.");
            }
        } else {
            String mobile = digitsOnly(request.mobileNumber());
            if (!FIJI_MOBILE.matcher(mobile).matches()) {
                errors.add("Enter the 7-digit mobile number linked to your "
                        + request.type().getDisplayName() + " wallet.");
            }
        }
        if (!errors.isEmpty()) {
            throw new PaymentValidationException(errors);
        }
    }

    /**
     * Validates a payment just before it is sent to the provider.
     *
     * @throws PaymentValidationException if the payment cannot go ahead
     */
    public void validateForPayment(PaymentMethod method, String cvv, BigDecimal amount) {
        List<String> errors = new ArrayList<>();
        if (!method.isActive()) {
            errors.add("This payment method has been removed. Choose another one.");
        }
        if (amount == null || amount.signum() <= 0) {
            errors.add("Payment amount must be greater than zero.");
        }
        if (method.getType().isCard()) {
            YearMonth expiry = YearMonth.of(method.getExpiryYear(), method.getExpiryMonth());
            if (expiry.isBefore(YearMonth.now(clock))) {
                errors.add("This card has expired. Add a new card.");
            }
            if (cvv == null || !CVV.matcher(cvv.trim()).matches()) {
                errors.add("Enter the 3-digit security code (CVV) from the back of the card.");
            }
        }
        if (!errors.isEmpty()) {
            throw new PaymentValidationException(errors);
        }
    }

    /**
     * Luhn checksum: catches most typing mistakes in card numbers.
     *
     * @param digits card number with digits only
     * @return true if the checksum is valid
     */
    public static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    /** @return "VISA", "MASTERCARD", or null if the brand is not accepted */
    public static String detectBrand(String digits) {
        if (digits.startsWith("4")) {
            return "VISA";
        }
        if (digits.length() >= 2) {
            int firstTwo = Integer.parseInt(digits.substring(0, 2));
            if (firstTwo >= 51 && firstTwo <= 55) {
                return "MASTERCARD";
            }
        }
        if (digits.length() >= 4) {
            int firstFour = Integer.parseInt(digits.substring(0, 4));
            if (firstFour >= 2221 && firstFour <= 2720) {
                return "MASTERCARD";
            }
        }
        return null;
    }

    /** @return the expiry as a YearMonth, or null if the format is wrong */
    public static YearMonth parseExpiry(String expiry) {
        if (expiry == null) {
            return null;
        }
        Matcher m = EXPIRY.matcher(expiry.trim());
        if (!m.matches()) {
            return null;
        }
        return YearMonth.of(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
    }

    /** Removes spaces, dashes and anything else that is not a digit. */
    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
