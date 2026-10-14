package net.codejava.payment.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns payment exceptions into clear JSON error responses
 * (Payment Error Handling). Customers never see a stack trace.
 */
@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(PaymentValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler({PaymentMethodNotFoundException.class, TransactionNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(PaymentException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RefundNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleRefund(RefundNotAllowedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    /** Any other payment problem. */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handleOther(PaymentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, List<String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        body.put("errors", errors);
        return ResponseEntity.status(status).body(body);
    }
}
