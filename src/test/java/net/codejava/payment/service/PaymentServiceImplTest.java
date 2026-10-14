package net.codejava.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import net.codejava.cart.dto.CartDto;
import net.codejava.cart.dto.CartItemDto;
import net.codejava.cart.dto.CheckoutValidationResult;
import net.codejava.cart.model.CartStatus;
import net.codejava.cart.service.CartService;
import net.codejava.payment.config.PaymentSettings;
import net.codejava.payment.dto.PaymentRequest;
import net.codejava.payment.dto.RefundRequest;
import net.codejava.payment.dto.TransactionDto;
import net.codejava.payment.exception.PaymentValidationException;
import net.codejava.payment.exception.RefundNotAllowedException;
import net.codejava.payment.gateway.SimulatedPaymentGateway;
import net.codejava.payment.model.LedgerEntry;
import net.codejava.payment.model.PaymentMethod;
import net.codejava.payment.model.PaymentMethodType;
import net.codejava.payment.model.PaymentTransaction;
import net.codejava.payment.model.TransactionStatus;
import net.codejava.payment.repository.LedgerEntryRepository;
import net.codejava.payment.repository.PaymentMethodRepository;
import net.codejava.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import net.codejava.payment.event.PaymentSucceededEvent;

/**
 * Tests for Transaction Processing, Duplicate Payment Prevention,
 * Refund Processing and Payment Error Handling. Uses the simulated
 * gateway and mocked repositories (no database).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String CUSTOMER = "customer1";

    @Mock private PaymentMethodRepository methodRepository;
    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private LedgerEntryRepository ledgerRepository;
    @Mock private CartService cartService;
    @Mock private ApplicationEventPublisher events;

    private PaymentServiceImpl paymentService;
    private PaymentMethod visa;
    private PaymentMethod declinedCard;

    @BeforeEach
    void setUp() {
        PaymentSettings settings = new PaymentSettings(new BigDecimal("7.50"), new BigDecimal("100.00"),
                new BigDecimal("0.015"), new BigDecimal("1000.00"), "STORE");
        paymentService = new PaymentServiceImpl(methodRepository, transactionRepository, ledgerRepository,
                cartService, new PaymentValidator(), new PaymentCalculator(settings),
                new SimulatedPaymentGateway(settings), settings, events);

        visa = PaymentMethod.card(CUSTOMER, "Camari K", "VISA", "4242", 8, 2099);
        visa.setId(1L);
        declinedCard = PaymentMethod.card(CUSTOMER, "Camari K", "VISA", "0002", 8, 2099);
        declinedCard.setId(2L);
    }

    /** A valid cart worth FJ$49.90 (2 x rice). */
    private void validCart() {
        when(cartService.validateForCheckout(CUSTOMER)).thenReturn(new CheckoutValidationResult(
                true, List.of(), new BigDecimal("49.90"), new BigDecimal("20.00")));
        when(cartService.getCart(CUSTOMER)).thenReturn(new CartDto(CUSTOMER, CartStatus.ACTIVE,
                List.of(new CartItemDto(9L, "Long Grain Rice 10kg", null, new BigDecimal("24.95"), 2, new BigDecimal("49.90"))),
                2, new BigDecimal("49.90"), null));
    }

    private void saveReturnsArgument() {
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulCardPayment_movesMoneyAndEmptiesCart() {
        validCart();
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(methodRepository.findByIdAndCustomerId(1L, CUSTOMER)).thenReturn(Optional.of(visa));
        saveReturnsArgument();

        TransactionDto result = paymentService.pay(CUSTOMER, new PaymentRequest(1L, "123", "key-1"));

        assertEquals(TransactionStatus.SUCCESS, result.status());
        assertEquals(new BigDecimal("58.26"), result.totalAmount()); // 49.90 + 7.50 delivery + 0.86 card fee
        verify(ledgerRepository).save(any(LedgerEntry.class));     // Money Transfer recorded
        verify(cartService).completeCheckout(CUSTOMER);            // stock reduced, cart emptied
        verify(events).publishEvent(any(PaymentSucceededEvent.class)); // Module 6 creates the order
    }

    @Test
    void declinedCard_isRecordedAsFailedAndNothingMoves() {
        validCart();
        when(transactionRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(methodRepository.findByIdAndCustomerId(2L, CUSTOMER)).thenReturn(Optional.of(declinedCard));
        saveReturnsArgument();

        TransactionDto result = paymentService.pay(CUSTOMER, new PaymentRequest(2L, "123", "key-2"));

        assertEquals(TransactionStatus.FAILED, result.status());
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
        verify(cartService, never()).completeCheckout(CUSTOMER);
        verify(events, never()).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    void samePaymentSentTwice_isNotChargedAgain() {
        PaymentTransaction first = new PaymentTransaction("SPM-1", "key-3", CUSTOMER, visa,
                new PaymentCalculator(new PaymentSettings(new BigDecimal("7.50"), new BigDecimal("100.00"),
                        new BigDecimal("0.015"), new BigDecimal("1000.00"), "STORE"))
                        .calculate(new BigDecimal("49.90"), PaymentMethodType.CARD));
        first.markSucceeded("SIM-1", "Approved");
        when(transactionRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.of(first));

        TransactionDto result = paymentService.pay(CUSTOMER, new PaymentRequest(1L, "123", "key-3"));

        assertTrue(result.duplicate());
        assertEquals("SPM-1", result.reference());
        verify(cartService, never()).validateForCheckout(CUSTOMER);
    }

    @Test
    void invalidCart_isRejectedBeforeAnyCharge() {
        when(transactionRepository.findByIdempotencyKey("key-4")).thenReturn(Optional.empty());
        when(cartService.validateForCheckout(CUSTOMER)).thenReturn(new CheckoutValidationResult(
                false, List.of("Cart is empty."), new BigDecimal("0.00"), new BigDecimal("20.00")));

        assertThrows(PaymentValidationException.class,
                () -> paymentService.pay(CUSTOMER, new PaymentRequest(1L, "123", "key-4")));
    }

    @Test
    void refund_partialThenRest_updatesStatus() {
        PaymentTransaction tx = new PaymentTransaction("SPM-5", "key-5", CUSTOMER, visa,
                new PaymentCalculator(new PaymentSettings(new BigDecimal("7.50"), new BigDecimal("100.00"),
                        BigDecimal.ZERO, new BigDecimal("1000.00"), "STORE"))
                        .calculate(new BigDecimal("50.00"), PaymentMethodType.CARD)); // total 57.50
        tx.markSucceeded("SIM-5", "Approved");
        when(transactionRepository.findByReference("SPM-5")).thenReturn(Optional.of(tx));
        saveReturnsArgument();

        TransactionDto partial = paymentService.refund("SPM-5", new RefundRequest(new BigDecimal("20.00"), "Damaged item"));
        assertEquals(TransactionStatus.PARTIALLY_REFUNDED, partial.status());

        TransactionDto full = paymentService.refund("SPM-5", new RefundRequest(null, "Order cancelled"));
        assertEquals(TransactionStatus.REFUNDED, full.status());
        assertFalse(full.refundable());
    }

    @Test
    void refund_moreThanPaid_isRejected() {
        PaymentTransaction tx = new PaymentTransaction("SPM-6", "key-6", CUSTOMER, visa,
                new PaymentCalculator(new PaymentSettings(new BigDecimal("7.50"), new BigDecimal("100.00"),
                        BigDecimal.ZERO, new BigDecimal("1000.00"), "STORE"))
                        .calculate(new BigDecimal("50.00"), PaymentMethodType.CARD));
        tx.markSucceeded("SIM-6", "Approved");
        when(transactionRepository.findByReference("SPM-6")).thenReturn(Optional.of(tx));

        assertThrows(RefundNotAllowedException.class,
                () -> paymentService.refund("SPM-6", new RefundRequest(new BigDecimal("999.00"), "Too much")));
    }
}
