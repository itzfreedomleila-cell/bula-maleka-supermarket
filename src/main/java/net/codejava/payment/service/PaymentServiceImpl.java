package net.codejava.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.codejava.cart.dto.CartDto;
import net.codejava.cart.dto.CheckoutValidationResult;
import net.codejava.cart.service.CartService;
import net.codejava.payment.config.PaymentSettings;
import net.codejava.payment.dto.AmountBreakdown;
import net.codejava.payment.dto.LedgerDto;
import net.codejava.payment.dto.MethodTypeDto;
import net.codejava.payment.dto.PaymentMethodDto;
import net.codejava.payment.dto.PaymentRequest;
import net.codejava.payment.dto.RefundRequest;
import net.codejava.payment.dto.RegisterPaymentMethodRequest;
import net.codejava.payment.dto.TransactionDto;
import net.codejava.payment.exception.PaymentException;
import net.codejava.payment.exception.PaymentMethodNotFoundException;
import net.codejava.payment.exception.PaymentValidationException;
import net.codejava.payment.exception.RefundNotAllowedException;
import net.codejava.payment.exception.TransactionNotFoundException;
import net.codejava.payment.gateway.GatewayResult;
import net.codejava.payment.gateway.PaymentGateway;
import net.codejava.payment.model.LedgerEntry;
import net.codejava.payment.model.LedgerEntryType;
import net.codejava.payment.model.PaymentMethod;
import net.codejava.payment.model.PaymentMethodType;
import net.codejava.payment.model.PaymentTransaction;
import net.codejava.payment.model.Refund;
import net.codejava.payment.model.TransactionLine;
import net.codejava.payment.model.TransactionStatus;
import net.codejava.payment.repository.LedgerEntryRepository;
import net.codejava.payment.repository.PaymentMethodRepository;
import net.codejava.payment.repository.PaymentTransactionRepository;
import net.codejava.account.event.AccountDeletedEvent;
import net.codejava.payment.event.PaymentRefundedEvent;
import net.codejava.payment.event.PaymentSucceededEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for Module 5. Uses Module 4 only through the CartService
 * interface, so the two modules stay loosely coupled.
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentServiceImpl.class.getName());
    private static final String REFERENCE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentMethodRepository methodRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final CartService cartService;
    private final PaymentValidator validator;
    private final PaymentCalculator calculator;
    private final PaymentGateway gateway;
    private final PaymentSettings settings;
    private final ApplicationEventPublisher events;

    public PaymentServiceImpl(PaymentMethodRepository methodRepository,
                              PaymentTransactionRepository transactionRepository,
                              LedgerEntryRepository ledgerRepository,
                              CartService cartService,
                              PaymentValidator validator,
                              PaymentCalculator calculator,
                              PaymentGateway gateway,
                              PaymentSettings settings,
                              ApplicationEventPublisher events) {
        this.methodRepository = methodRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.cartService = cartService;
        this.validator = validator;
        this.calculator = calculator;
        this.gateway = gateway;
        this.settings = settings;
        this.events = events;
    }

    // ---------------------------------------------------------------
    // Payment Registration and Method Selection
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<MethodTypeDto> listMethodTypes() {
        return Arrays.stream(PaymentMethodType.values())
                .map(t -> new MethodTypeDto(t, t.getDisplayName(), t.isCard(),
                        t.isCard() ? settings.getCardSurchargeRate() : BigDecimal.ZERO))
                .toList();
    }

    @Override
    public PaymentMethodDto registerPaymentMethod(String customerId, RegisterPaymentMethodRequest request) {
        requireCustomer(customerId);
        validator.validateRegistration(request);

        String holder = request.holderName().trim();
        PaymentMethod method;
        if (request.type().isCard()) {
            String digits = PaymentValidator.digitsOnly(request.cardNumber());
            var expiry = PaymentValidator.parseExpiry(request.expiry());
            method = PaymentMethod.card(customerId, holder, PaymentValidator.detectBrand(digits),
                    digits.substring(digits.length() - 4), expiry.getMonthValue(), expiry.getYear());
        } else {
            method = PaymentMethod.wallet(customerId, request.type(), holder,
                    PaymentValidator.digitsOnly(request.mobileNumber()));
        }
        return PaymentMethodDto.from(methodRepository.save(method));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodDto> listPaymentMethods(String customerId) {
        requireCustomer(customerId);
        return methodRepository.findByCustomerIdAndActiveTrueOrderByCreatedAtDesc(customerId).stream()
                .map(PaymentMethodDto::from)
                .toList();
    }

    @Override
    public void removePaymentMethod(String customerId, Long methodId) {
        PaymentMethod method = findCustomerMethod(customerId, methodId);
        method.deactivate();
        methodRepository.save(method);
    }

    // ---------------------------------------------------------------
    // Payment Amount Calculation
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AmountBreakdown quote(String customerId, Long methodId) {
        PaymentMethodType type = methodId == null ? null : findCustomerMethod(customerId, methodId).getType();
        return calculator.calculate(cartService.calculateCartTotal(customerId), type);
    }

    // ---------------------------------------------------------------
    // Transaction Processing (with validation, money transfer,
    // confirmation and duplicate prevention)
    // ---------------------------------------------------------------

    @Override
    public TransactionDto pay(String customerId, PaymentRequest request) {
        requireCustomer(customerId);
        if (request == null || request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new PaymentValidationException(List.of("Payment request id is missing. Refresh the page and try again."));
        }

        // 1. Duplicate Payment Prevention: same request id = return the first result, don't charge again
        Optional<PaymentTransaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            PaymentTransaction previous = existing.get();
            if (!previous.getCustomerId().equals(customerId)) {
                throw new PaymentException("This payment request id has already been used.");
            }
            return TransactionDto.from(previous, true);
        }

        // 2. Payment Validation: the cart (Module 4 rules) and the payment details
        CheckoutValidationResult cartCheck = cartService.validateForCheckout(customerId);
        if (!cartCheck.valid()) {
            throw new PaymentValidationException(cartCheck.errors());
        }
        PaymentMethod method = findCustomerMethod(customerId, request.paymentMethodId());
        CartDto cart = cartService.getCart(customerId);
        AmountBreakdown amount = calculator.calculate(cart.total(), method.getType());
        validator.validateForPayment(method, request.cvv(), amount.total());

        // 3. Create the transaction with a copy of what is being bought
        PaymentTransaction transaction = new PaymentTransaction(
                generateReference(), request.idempotencyKey(), customerId, method, amount);
        cart.items().forEach(item -> transaction.addLine(new TransactionLine(
                item.productName(), item.quantity(), item.unitPrice(), item.subtotal())));
        transaction.changeStatus(TransactionStatus.PROCESSING,
                "Sent to " + method.getType().getDisplayName() + " for approval.");

        // 4. Ask the payment provider
        GatewayResult result;
        try {
            result = gateway.charge(method, amount.total(), transaction.getReference());
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Payment provider error for " + transaction.getReference(), ex);
            result = GatewayResult.declined("The payment provider is not responding. You were not charged. Please try again.");
        }

        // 5. Confirm and move the money, or record the failure
        if (result.approved()) {
            transaction.markSucceeded(result.gatewayReference(),
                    "Approved. FJ$" + amount.total() + " received (provider ref " + result.gatewayReference() + ").");
            ledgerRepository.save(new LedgerEntry(transaction.getReference(), LedgerEntryType.PAYMENT,
                    method.getDisplayLabel(), settings.getStoreAccount(), amount.total()));
            cartService.completeCheckout(customerId);
            events.publishEvent(new PaymentSucceededEvent(transaction.getReference(), customerId,
                    method.getDisplayLabel(), amount.subtotal(), amount.deliveryFee(), amount.surcharge(),
                    amount.total(), cart.items().stream()
                            .map(i -> new PaymentSucceededEvent.Line(i.productId(), i.productName(),
                                    i.quantity(), i.unitPrice(), i.subtotal()))
                            .toList()));
        } else {
            transaction.markFailed(result.message());
        }
        return TransactionDto.from(transactionRepository.save(transaction), false);
    }

    // ---------------------------------------------------------------
    // Status Tracking and History
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TransactionDto getTransaction(String reference) {
        return TransactionDto.from(findTransaction(reference), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getHistory(String customerId) {
        requireCustomer(customerId);
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(t -> TransactionDto.from(t, false))
                .toList();
    }

    // ---------------------------------------------------------------
    // Refund Processing
    // ---------------------------------------------------------------

    @Override
    public TransactionDto refund(String reference, RefundRequest request) {
        PaymentTransaction transaction = findTransaction(reference);
        if (!transaction.canBeRefunded()) {
            throw new RefundNotAllowedException("Only successful payments that are not fully refunded can be refunded. "
                    + "This one is " + transaction.getStatus() + ".");
        }
        BigDecimal refundable = transaction.getRefundableAmount();
        BigDecimal amount = (request == null || request.amount() == null)
                ? refundable
                : request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0 || amount.compareTo(refundable) > 0) {
            throw new RefundNotAllowedException("Refund amount must be between FJ$0.01 and FJ$" + refundable + ".");
        }
        String reason = (request == null || request.reason() == null || request.reason().isBlank())
                ? "Customer request"
                : request.reason().trim();

        GatewayResult result = gateway.refund(transaction.getGatewayReference(), amount);
        if (!result.approved()) {
            throw new PaymentException("Refund could not be processed: " + result.message());
        }
        transaction.addRefund(new Refund(amount, reason, result.gatewayReference()));
        ledgerRepository.save(new LedgerEntry(reference, LedgerEntryType.REFUND,
                settings.getStoreAccount(), transaction.getMethodLabel(), amount));
        PaymentTransaction saved = transactionRepository.save(transaction);
        events.publishEvent(new PaymentRefundedEvent(reference, amount, saved.getRefundedAmount(),
                saved.getStatus() == TransactionStatus.REFUNDED));
        return TransactionDto.from(saved, false);
    }

    // ---------------------------------------------------------------
    // Money Transfer ledger
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public LedgerDto getLedger() {
        List<LedgerEntry> entries = ledgerRepository.findAllByOrderByCreatedAtDesc();
        BigDecimal balance = BigDecimal.ZERO.setScale(2);
        for (LedgerEntry e : entries) {
            balance = e.getType() == LedgerEntryType.PAYMENT ? balance.add(e.getAmount()) : balance.subtract(e.getAmount());
        }
        return new LedgerDto(settings.getStoreAccount(), balance,
                entries.stream().map(LedgerDto.EntryDto::from).toList());
    }

    /** When a customer deletes their account, their saved payment methods are removed. */
    @EventListener
    public void onAccountDeleted(AccountDeletedEvent event) {
        methodRepository.findByCustomerIdAndActiveTrueOrderByCreatedAtDesc(event.customerId())
                .forEach(PaymentMethod::deactivate);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private PaymentMethod findCustomerMethod(String customerId, Long methodId) {
        if (methodId == null) {
            throw new PaymentValidationException(List.of("Choose a payment method."));
        }
        return methodRepository.findByIdAndCustomerId(methodId, customerId)
                .orElseThrow(() -> new PaymentMethodNotFoundException(methodId));
    }

    private PaymentTransaction findTransaction(String reference) {
        return transactionRepository.findByReference(reference)
                .orElseThrow(() -> new TransactionNotFoundException(reference));
    }

    private void requireCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new PaymentValidationException(List.of("A customer id is required."));
        }
    }

    /** @return a reference like SPM-20261008-7K3QXA (no easily confused letters) */
    private String generateReference() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(REFERENCE_CHARS.charAt(RANDOM.nextInt(REFERENCE_CHARS.length())));
        }
        return "SPM-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + code;
    }
}
