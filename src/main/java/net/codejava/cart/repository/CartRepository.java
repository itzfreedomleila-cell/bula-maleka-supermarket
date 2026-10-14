package net.codejava.cart.repository;

import java.util.Optional;
import net.codejava.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for shopping carts.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /** @return the cart belonging to a customer, if one exists */
    Optional<Cart> findByCustomerId(String customerId);
}
