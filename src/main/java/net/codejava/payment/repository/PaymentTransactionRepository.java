package net.codejava.payment.repository;

import java.util.List;
import java.util.Optional;
import net.codejava.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for payment transactions. */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByReference(String reference);

    /** Used for Duplicate Payment Prevention. */
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    /** Used for Transaction History (newest first). */
    List<PaymentTransaction> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
