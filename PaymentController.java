package net.codejava.payment.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import net.codejava.account.security.CurrentUser;
import net.codejava.common.exception.AccessDeniedException;
import net.codejava.payment.dto.AmountBreakdown;
import net.codejava.payment.dto.MethodTypeDto;
import net.codejava.payment.dto.PaymentMethodDto;
import net.codejava.payment.dto.PaymentRequest;
import net.codejava.payment.dto.RegisterPaymentMethodRequest;
import net.codejava.payment.dto.TransactionDto;
import net.codejava.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for Module 5 - Payment Processing (customer side).
 * The customer always comes from the login session.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUser currentUser;

    public PaymentController(PaymentService paymentService, CurrentUser currentUser) {
        this.paymentService = paymentService;
        this.currentUser = currentUser;
    }

    /** Payment Method Selection: accepted method types. */
    @GetMapping("/method-types")
    public List<MethodTypeDto> methodTypes() {
        return paymentService.listMethodTypes();
    }

    /** Payment Registration. */
    @PostMapping("/methods")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodDto register(@RequestBody RegisterPaymentMethodRequest request, HttpSession session) {
        return paymentService.registerPaymentMethod(currentUser.customerId(session), request);
    }

    /** Payment Method Selection: the customer's saved methods. */
    @GetMapping("/methods")
    public List<PaymentMethodDto> methods(HttpSession session) {
        return paymentService.listPaymentMethods(currentUser.customerId(session));
    }

    @DeleteMapping("/methods/{methodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("methodId") Long methodId, HttpSession session) {
        paymentService.removePaymentMethod(currentUser.customerId(session), methodId);
    }

    /** Payment Amount Calculation. */
    @GetMapping("/quote")
    public AmountBreakdown quote(@RequestParam(value = "methodId", required = false) Long methodId,
                                 HttpSession session) {
        return paymentService.quote(currentUser.customerId(session), methodId);
    }

    /** Transaction Processing, Confirmation, Money Transfer, Duplicate Prevention. */
    @PostMapping("/pay")
    public TransactionDto pay(@RequestBody PaymentRequest request, HttpSession session) {
        return paymentService.pay(currentUser.customerId(session), request);
    }

    /** Transaction History. */
    @GetMapping("/transactions")
    public List<TransactionDto> history(HttpSession session) {
        return paymentService.getHistory(currentUser.customerId(session));
    }

    /** Transaction Status Tracking (own payments only, or any for staff). */
    @GetMapping("/transactions/{reference}")
    public TransactionDto transaction(@PathVariable("reference") String reference, HttpSession session) {
        TransactionDto tx = paymentService.getTransaction(reference);
        if (!currentUser.canView(session, tx.customerId())) {
            throw new AccessDeniedException("You can only view your own payments.");
        }
        return tx;
    }
}
