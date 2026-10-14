package net.codejava.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.codejava.catalog.model.Category;
import net.codejava.catalog.model.Product;
import java.util.Optional;
import net.codejava.cart.dto.CartDto;
import net.codejava.cart.dto.CheckoutValidationResult;
import net.codejava.cart.exception.CartException;
import net.codejava.cart.exception.InsufficientStockException;
import net.codejava.cart.exception.InvalidQuantityException;
import net.codejava.cart.model.Cart;
import net.codejava.cart.model.CartItem;
import net.codejava.cart.model.CartStatus;
import net.codejava.cart.repository.CartRepository;
import net.codejava.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CartServiceImpl. The repositories are mocked with Mockito,
 * so these tests check the business rules without a real database.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    private static final String CUSTOMER = "customer1";
    private static final BigDecimal MINIMUM_ORDER = new BigDecimal("20.00");

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    private CartServiceImpl cartService;
    private Product rice;
    private Product milk;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartRepository, productRepository, MINIMUM_ORDER);
        rice = new Product("Long Grain Rice 10kg", null, new Category("Pantry", null), new BigDecimal("24.95"), 10, null);
        rice.setId(9L);
        milk = new Product("Fresh Milk 1L", null, new Category("Dairy & Eggs", null), new BigDecimal("3.20"), 60, null);
        milk.setId(4L);
    }

    /** Makes cartRepository.save(...) return the cart it was given. */
    private void saveReturnsArgument() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Cart cartWith(Product product, int quantity) {
        Cart cart = new Cart(CUSTOMER);
        cart.addItem(new CartItem(product, quantity));
        return cart;
    }

    // ---------- Add Product ----------

    @Test
    void addProduct_newProduct_isAddedWithCorrectTotal() {
        when(productRepository.findById(9L)).thenReturn(Optional.of(rice));
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.empty());
        saveReturnsArgument();

        CartDto result = cartService.addProduct(CUSTOMER, 9L, 2);

        assertEquals(1, result.items().size());
        assertEquals(2, result.items().get(0).quantity());
        assertEquals(new BigDecimal("49.90"), result.total());
    }

    @Test
    void addProduct_alreadyInCart_increasesQuantity() {
        when(productRepository.findById(9L)).thenReturn(Optional.of(rice));
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWith(rice, 2)));
        saveReturnsArgument();

        CartDto result = cartService.addProduct(CUSTOMER, 9L, 3);

        assertEquals(1, result.items().size());
        assertEquals(5, result.items().get(0).quantity());
    }

    @Test
    void addProduct_moreThanStock_isRejected() {
        when(productRepository.findById(9L)).thenReturn(Optional.of(rice));
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(new Cart(CUSTOMER)));

        assertThrows(InsufficientStockException.class,
                () -> cartService.addProduct(CUSTOMER, 9L, 11));
    }

    @Test
    void addProduct_zeroQuantity_isRejected() {
        assertThrows(InvalidQuantityException.class,
                () -> cartService.addProduct(CUSTOMER, 9L, 0));
    }

    // ---------- Remove Product ----------

    @Test
    void removeProduct_removesTheLine() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWith(rice, 2)));
        saveReturnsArgument();

        CartDto result = cartService.removeProduct(CUSTOMER, 9L);

        assertTrue(result.items().isEmpty());
        assertEquals(new BigDecimal("0.00"), result.total());
    }

    // ---------- Update Product Quantity ----------

    @Test
    void updateQuantity_changesSubtotal() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWith(milk, 1)));
        saveReturnsArgument();

        CartDto result = cartService.updateQuantity(CUSTOMER, 4L, 4);

        assertEquals(new BigDecimal("12.80"), result.items().get(0).subtotal());
    }

    // ---------- Calculate Subtotal / Cart Total ----------

    @Test
    void calculateSubtotalAndTotal_returnCorrectAmounts() {
        Cart cart = cartWith(rice, 2);            // 49.90
        cart.addItem(new CartItem(milk, 3));      // 9.60
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cart));

        assertEquals(new BigDecimal("49.90"), cartService.calculateSubtotal(CUSTOMER, 9L));
        assertEquals(new BigDecimal("59.50"), cartService.calculateCartTotal(CUSTOMER));
    }

    // ---------- Validate Cart for Checkout ----------

    @Test
    void validateForCheckout_emptyCart_isInvalid() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(new Cart(CUSTOMER)));

        CheckoutValidationResult result = cartService.validateForCheckout(CUSTOMER);

        assertFalse(result.valid());
        assertEquals(1, result.errors().size());
    }

    @Test
    void validateForCheckout_belowMinimumOrder_isInvalid() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWith(milk, 2))); // 6.40

        CheckoutValidationResult result = cartService.validateForCheckout(CUSTOMER);

        assertFalse(result.valid());
        assertTrue(result.errors().get(0).contains("13.60")); // 20.00 - 6.40
    }

    @Test
    void validateForCheckout_stockDroppedBelowCartQuantity_isInvalid() {
        Cart cart = cartWith(rice, 3);
        rice.setStockQuantity(1); // other shoppers bought most of the stock
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cart));

        CheckoutValidationResult result = cartService.validateForCheckout(CUSTOMER);

        assertFalse(result.valid());
    }

    @Test
    void validateForCheckout_goodCart_isValid() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWith(rice, 2)));

        CheckoutValidationResult result = cartService.validateForCheckout(CUSTOMER);

        assertTrue(result.valid());
        assertEquals(new BigDecimal("49.90"), result.total());
    }

    // ---------- Save and Restore Cart ----------

    @Test
    void saveThenRestore_changesStatus() {
        Cart cart = cartWith(rice, 1);
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cart));
        saveReturnsArgument();

        assertEquals(CartStatus.SAVED, cartService.saveCart(CUSTOMER).status());
        assertEquals(CartStatus.ACTIVE, cartService.restoreCart(CUSTOMER).status());
    }

    // ---------- Checkout completion (used by Module 5) ----------

    @Test
    void completeCheckout_reducesStockAndEmptiesCart() {
        Cart cart = cartWith(rice, 3);
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cart));
        saveReturnsArgument();

        cartService.completeCheckout(CUSTOMER);

        assertEquals(7, rice.getStockQuantity()); // 10 - 3
        assertTrue(cart.isEmpty());
    }

    @Test
    void restore_withoutSave_isRejected() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWith(rice, 1)));

        assertThrows(CartException.class, () -> cartService.restoreCart(CUSTOMER));
    }
}
