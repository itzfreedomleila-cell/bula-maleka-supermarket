package net.codejava.payment.controller;

import jakarta.servlet.http.HttpSession;
import net.codejava.account.security.CurrentUser;
import net.codejava.payment.dto.LedgerDto;
import net.codejava.payment.dto.RefundRequest;
import net.codejava.payment.dto.TransactionDto;
import net.codejava.payment.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff-only payment actions: refunds and the money transfer ledger. */
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final CurrentUser currentUser;

    public AdminPaymentController(PaymentService paymentService, CurrentUser currentUser) {
        this.paymentService = paymentService;
        this.currentUser = currentUser;
    }

    /** Refund Processing. Leave amount empty for a full refund. */
    @PostMapping("/{reference}/refund")
    public TransactionDto refund(@PathVariable("reference") String reference,
                                 @RequestBody(required = false) RefundRequest request, HttpSession session) {
        currentUser.requireAdmin(session);
        return paymentService.refund(reference, request);
    }

    /** Money Transfer records and store balance. */
    @GetMapping("/ledger")
    public LedgerDto ledger(HttpSession session) {
        currentUser.requireAdmin(session);
        return paymentService.getLedger();
    }

    /** Status tracking for any payment. */
    @GetMapping("/{reference}")
    public TransactionDto transaction(@PathVariable("reference") String reference, HttpSession session) {
        currentUser.requireAdmin(session);
        return paymentService.getTransaction(reference);
    }
}
