package net.codejava.cart.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts shopping cart exceptions into clear JSON error responses.
 */
@RestControllerAdvice
public class CartExceptionHandler {

    @ExceptionHandler({CartNotFoundException.class, ProductNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(CartException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleStock(InsufficientStockException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Catches InvalidQuantityException and any other CartException. */
    @ExceptionHandler(CartException.class)
    public ResponseEntity<Map<String, Object>> handleCart(CartException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }


    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
