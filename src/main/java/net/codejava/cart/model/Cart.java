package net.codejava.cart.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A customer's shopping cart. Each customer has exactly one cart, which is
 * stored in the database so it can be saved and restored later.
 */
@Entity
@Table(name = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Required by JPA. */
    protected Cart() {
    }

    public Cart(String customerId) {
        this.customerId = customerId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Finds the cart line for a product, if the product is already in the cart.
     *
     * @param productId id of the product
     * @return the matching item, or empty if not in the cart
     */
    public Optional<CartItem> findItem(Long productId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getProduct().getId(), productId))
                .findFirst();
    }

    /** Adds a new line to the cart and links it back to this cart. */
    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
        touch();
    }

    /** Removes a line from the cart (orphanRemoval deletes it from the DB). */
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
        touch();
    }

    /** Empties the cart after a successful payment. */
    public void clear() {
        items.forEach(item -> item.setCart(null));
        items.clear();
        touch();
    }

    /**
     * Calculates the cart total: the sum of every line subtotal.
     *
     * @return cart total rounded to 2 decimal places
     */
    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** @return total number of units across all lines */
    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Marks the cart as saved and records when. */
    public void markSaved() {
        this.status = CartStatus.SAVED;
        this.savedAt = LocalDateTime.now();
        touch();
    }

    /** Puts the cart back into active use (e.g. after restoring it). */
    public void markActive() {
        this.status = CartStatus.ACTIVE;
        touch();
    }

    /** Updates the "last changed" timestamp. */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public CartStatus getStatus() {
        return status;
    }

    /** @return a read-only view so items can only change through Cart methods */
    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
