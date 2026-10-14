package net.codejava.payment.service;

import java.util.List;
import net.codejava.payment.dto.AmountBreakdown;
import net.codejava.payment.dto.LedgerDto;
import net.codejava.payment.dto.MethodTypeDto;
import net.codejava.payment.dto.PaymentMethodDto;
import net.codejava.payment.dto.PaymentRequest;
import net.codejava.payment.dto.RefundRequest;
import net.codejava.payment.dto.RegisterPaymentMethodRequest;
import net.codejava.payment.dto.TransactionDto;

/**
 * Module 5 - Payment Processing operations.
 */
public interface PaymentService {

    /** Payment Method Selection: the method types the store accepts. */
    List<MethodTypeDto> listMethodTypes();

    /** Payment Registration: saves a new card or wallet for a customer. */
    PaymentMethodDto registerPaymentMethod(String customerId, RegisterPaymentMethodRequest request);

    /** Payment Method Selection: the customer's registered methods. */
    List<PaymentMethodDto> listPaymentMethods(String customerId);

    /** Removes (deactivates) a registered payment method. */
    void removePaymentMethod(String customerId, Long methodId);

    /** Payment Amount Calculation: what the customer would pay with this method. */
    AmountBreakdown quote(String customerId, Long methodId);

    /**
     * Transaction Processing: validates, charges, transfers money, confirms,
     * and prevents duplicate payments.
     */
    TransactionDto pay(String customerId, PaymentRequest request);

    /** Transaction Status Tracking: one transaction with its full status history. */
    TransactionDto getTransaction(String reference);

    /** Transaction History: all of a customer's transactions, newest first. */
    List<TransactionDto> getHistory(String customerId);

    /** Refund Processing: full or partial refund of a successful payment. */
    TransactionDto refund(String reference, RefundRequest request);

    /** Money Transfer: all money movements and the store's balance. */
    LedgerDto getLedger();
}
