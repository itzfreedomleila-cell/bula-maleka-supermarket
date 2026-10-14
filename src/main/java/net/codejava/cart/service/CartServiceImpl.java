package net.codejava.cart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.codejava.cart.dto.CartDto;
import net.codejava.cart.dto.CheckoutValidationResult;
import net.codejava.cart.exception.CartException;
import net.codejava.cart.exception.CartNotFoundException;
import net.codejava.cart.exception.InsufficientStockException;
import net.codejava.cart.exception.InvalidQuantityException;
import net.codejava.cart.exception.ProductNotFoundException;
import net.codejava.cart.model.Cart;
import net.codejava.cart.model.CartItem;
import net.codejava.cart.model.CartStatus;
import net.codejava.catalog.model.Product;
import net.codejava.cart.repository.CartRepository;
import net.codejava.catalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import net.codejava.account.event.AccountDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the shopping cart. Every method runs in a database
 * transaction, so a failed operation never leaves a half-updated cart.
 */
@Service
@Transactional
public class CartServiceImpl implements CartService {

    /** Upper limit per product line, to stop accidental huge orders. */
    static final int MAX_QUANTITY_PER_ITEM = 99;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    /** Minimum value for an online order, set in application.properties (cart.minimum-order). */
    private final BigDecimal minimumOrder;

    /**
     * Constructor injection: Spring passes in the repositories and the
     * minimum order value (defaults to 20.00 if the property is missing).
     */
    public CartServiceImpl(CartRepository cartRepository,
                           ProductRepository productRepository,
                           @Value("${cart.minimum-order:20.00}") BigDecimal minimumOrder) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.minimumOrder = minimumOrder.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public CartDto getCart(String customerId) {
        return CartDto.from(findOrCreateCart(customerId));
    }

    @Override
    public CartDto addProduct(String customerId, Long productId, int quantity) {
        validateQuantity(quantity);
        Product product = findActiveProduct(productId);
        Cart cart = findOrCreateCart(customerId);

        Optional<CartItem> existing = cart.findItem(productId);
        int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + quantity;
        validateQuantity(newQuantity);
        checkStock(product, newQuantity);

        if (existing.isPresent()) {
            existing.get().setQuantity(newQuantity);
        } else {
            cart.addItem(new CartItem(product, quantity));
        }
        cart.markActive();
        return CartDto.from(cartRepository.save(cart));
    }

    @Override
    public CartDto removeProduct(String customerId, Long productId) {
        Cart cart = findExistingCart(customerId);
        CartItem item = findItemInCart(cart, productId);
        cart.removeItem(item);
        return CartDto.from(cartRepository.save(cart));
    }

    @Override
    public CartDto updateQuantity(String customerId, Long productId, int quantity) {
        if (quantity == 0) {
            return removeProduct(customerId, productId);
        }
        validateQuantity(quantity);
        Cart cart = findExistingCart(customerId);
        CartItem item = findItemInCart(cart, productId);
        checkStock(item.getProduct(), quantity);

        item.setQuantity(quantity);
        cart.touch();
        return CartDto.from(cartRepository.save(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSubtotal(String customerId, Long productId) {
        Cart cart = findExistingCart(customerId);
        return findItemInCart(cart, productId).getSubtotal();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateCartTotal(String customerId) {
        return findExistingCart(customerId).getTotal();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutValidationResult validateForCheckout(String customerId) {
        List<String> errors = new ArrayList<>();
        Optional<Cart> found = cartRepository.findByCustomerId(customerId);

        if (found.isEmpty() || found.get().isEmpty()) {
            errors.add("Cart is empty. Add at least one product before checkout.");
            return new CheckoutValidationResult(false, errors, BigDecimal.ZERO.setScale(2), minimumOrder);
        }

        Cart cart = found.get();
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                errors.add("'" + product.getName() + "' is no longer available. Remove it to continue.");
            } else if (item.getQuantity() > product.getStockQuantity()) {
                errors.add("Only " + product.getStockQuantity() + " of '" + product.getName()
                        + "' in stock, but the cart has " + item.getQuantity() + ".");
            }
            if (item.getQuantity() <= 0) {
                errors.add("'" + product.getName() + "' has an invalid quantity.");
            }
        }

        BigDecimal total = cart.getTotal();
        if (total.signum() <= 0) {
            errors.add("Cart total must be greater than zero.");
        } else if (total.compareTo(minimumOrder) < 0) {
            BigDecimal shortBy = minimumOrder.subtract(total);
            errors.add("Online orders need a minimum of FJ$" + minimumOrder
                    + ". Add FJ$" + shortBy + " more to check out.");
        }
        return new CheckoutValidationResult(errors.isEmpty(), errors, total, minimumOrder);
    }

    @Override
    public CartDto saveCart(String customerId) {
        Cart cart = findExistingCart(customerId);
        if (cart.isEmpty()) {
            throw new CartException("Cannot save an empty cart.");
        }
        cart.markSaved();
        return CartDto.from(cartRepository.save(cart));
    }

    @Override
    public CartDto restoreCart(String customerId) {
        Cart cart = findExistingCart(customerId);
        if (cart.getStatus() != CartStatus.SAVED) {
            throw new CartException("No saved cart to restore for customer '" + customerId + "'.");
        }
        cart.markActive();
        return CartDto.from(cartRepository.save(cart));
    }

    @Override
    public void completeCheckout(String customerId) {
        Cart cart = findExistingCart(customerId);
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            checkStock(product, item.getQuantity());
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
        }
        cart.clear();
        cart.markActive();
        cartRepository.save(cart);
    }

    /** When a customer deletes their account, their cart is deleted too. */
    @EventListener
    public void onAccountDeleted(AccountDeletedEvent event) {
        cartRepository.findByCustomerId(event.customerId()).ifPresent(cartRepository::delete);
    }

    // ---------------------------------------------------------------
    // Helper methods (private: only used inside this class)
    // ---------------------------------------------------------------

    private Cart findOrCreateCart(String customerId) {
        validateCustomerId(customerId);
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(new Cart(customerId)));
    }

    private Cart findExistingCart(String customerId) {
        validateCustomerId(customerId);
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));
    }

    private Product findActiveProduct(Long productId) {
        if (productId == null) {
            throw new ProductNotFoundException("A product id is required.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product " + productId + " does not exist."));
        if (!product.isActive()) {
            throw new ProductNotFoundException("'" + product.getName() + "' is not available.");
        }
        return product;
    }

    private CartItem findItemInCart(Cart cart, Long productId) {
        return cart.findItem(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product " + productId + " is not in the cart."));
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_ITEM) {
            throw new InvalidQuantityException(
                    "Quantity must be between 1 and " + MAX_QUANTITY_PER_ITEM + ".");
        }
    }

    private void checkStock(Product product, int requested) {
        if (requested > product.getStockQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity());
        }
    }

    private void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new CartException("A customer id is required.");
        }
    }
}
