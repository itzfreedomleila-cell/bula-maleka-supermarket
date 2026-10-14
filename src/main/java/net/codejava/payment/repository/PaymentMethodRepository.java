package net.codejava.payment.repository;

import java.util.List;
import java.util.Optional;
import net.codejava.payment.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for registered payment methods. */
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByCustomerIdAndActiveTrueOrderByCreatedAtDesc(String customerId);

    Optional<PaymentMethod> findByIdAndCustomerId(Long id, String customerId);
}
