package net.codejava.cart.controller;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Map;
import net.codejava.account.security.CurrentUser;
import net.codejava.cart.dto.AddItemRequest;
import net.codejava.cart.dto.CartDto;
import net.codejava.cart.dto.CheckoutValidationResult;
import net.codejava.cart.dto.UpdateQuantityRequest;
import net.codejava.cart.service.CartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for Module 4 - Shopping Cart. Works on the logged-in
 * customer's cart (taken from the session, never from the URL).
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    public CartController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    /** View the cart. */
    @GetMapping
    public CartDto getCart(HttpSession session) {
        return cartService.getCart(currentUser.customerId(session));
    }

    /** Add Product. Body: {"productId": 1, "quantity": 2} */
    @PostMapping("/items")
    public CartDto addProduct(@RequestBody AddItemRequest request, HttpSession session) {
        return cartService.addProduct(currentUser.customerId(session), request.productId(), request.quantity());
    }

    /** Remove Product. */
    @DeleteMapping("/items/{productId}")
    public CartDto removeProduct(@PathVariable("productId") Long productId, HttpSession session) {
        return cartService.removeProduct(currentUser.customerId(session), productId);
    }

    /** Update Product Quantity. Body: {"quantity": 3} */
    @PutMapping("/items/{productId}")
    public CartDto updateQuantity(@PathVariable("productId") Long productId,
                                  @RequestBody UpdateQuantityRequest request, HttpSession session) {
        return cartService.updateQuantity(currentUser.customerId(session), productId, request.quantity());
    }

    /** Calculate Subtotal for one product. */
    @GetMapping("/items/{productId}/subtotal")
    public Map<String, Object> getSubtotal(@PathVariable("productId") Long productId, HttpSession session) {
        BigDecimal subtotal = cartService.calculateSubtotal(currentUser.customerId(session), productId);
        return Map.of("productId", productId, "subtotal", subtotal);
    }

    /** Calculate Cart Total. */
    @GetMapping("/total")
    public Map<String, Object> getTotal(HttpSession session) {
        return Map.of("total", cartService.calculateCartTotal(currentUser.customerId(session)));
    }

    /** Validate Cart for Checkout. */
    @GetMapping("/validate")
    public CheckoutValidationResult validate(HttpSession session) {
        return cartService.validateForCheckout(currentUser.customerId(session));
    }

    /** Save Cart. */
    @PostMapping("/save")
    public CartDto save(HttpSession session) {
        return cartService.saveCart(currentUser.customerId(session));
    }

    /** Restore Cart. */
    @PostMapping("/restore")
    public CartDto restore(HttpSession session) {
        return cartService.restoreCart(currentUser.customerId(session));
    }
}
