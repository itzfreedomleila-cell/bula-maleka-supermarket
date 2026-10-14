package net.codejava.order.repository;

import java.util.List;
import java.util.Optional;
import net.codejava.order.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for orders. */
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByPaymentReference(String paymentReference);

    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
