package net.codejava.cart.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import net.codejava.catalog.model.Category;
import net.codejava.catalog.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Cart and CartItem calculations (no database needed).
 */
class CartTest {

    private Product milk;
    private Product rice;
    private Cart cart;

    @BeforeEach
    void setUp() {
        milk = new Product("Fresh Milk 1L", null, new Category("Dairy & Eggs", null), new BigDecimal("3.20"), 60, null);
        milk.setId(4L);
        rice = new Product("Long Grain Rice 10kg", null, new Category("Pantry", null), new BigDecimal("24.95"), 30, null);
        rice.setId(9L);
        cart = new Cart("customer1");
    }

    @Test
    void subtotal_isUnitPriceTimesQuantity() {
        CartItem item = new CartItem(milk, 3);
        assertEquals(new BigDecimal("9.60"), item.getSubtotal());
    }

    @Test
    void total_isSumOfAllSubtotals() {
        cart.addItem(new CartItem(milk, 2));   // 6.40
        cart.addItem(new CartItem(rice, 1));   // 24.95
        assertEquals(new BigDecimal("31.35"), cart.getTotal());
        assertEquals(3, cart.getItemCount());
    }

    @Test
    void emptyCart_hasZeroTotal() {
        assertTrue(cart.isEmpty());
        assertEquals(new BigDecimal("0.00"), cart.getTotal());
    }

    @Test
    void removeItem_takesItOutOfTheTotal() {
        CartItem milkItem = new CartItem(milk, 2);
        cart.addItem(milkItem);
        cart.addItem(new CartItem(rice, 1));
        cart.removeItem(milkItem);
        assertEquals(new BigDecimal("24.95"), cart.getTotal());
        assertTrue(cart.findItem(4L).isEmpty());
    }

    @Test
    void markSaved_thenMarkActive_changesStatus() {
        cart.markSaved();
        assertEquals(CartStatus.SAVED, cart.getStatus());
        cart.markActive();
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
    }
}
