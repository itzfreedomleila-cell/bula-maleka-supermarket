package net.codejava.order.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import net.codejava.account.security.CurrentUser;
import net.codejava.common.exception.AccessDeniedException;
import net.codejava.order.dto.OrderDto;
import net.codejava.order.dto.ReceiptDto;
import net.codejava.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API for Module 6 - Order Processing. */
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUser currentUser;

    public OrderController(OrderService orderService, CurrentUser currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    /** The logged-in customer's orders. */
    @GetMapping("/orders")
    public List<OrderDto> myOrders(HttpSession session) {
        return orderService.listForCustomer(currentUser.customerId(session));
    }

    /** Order Confirmation details. */
    @GetMapping("/orders/{orderNumber}")
    public OrderDto order(@PathVariable("orderNumber") String orderNumber, HttpSession session) {
        return checkOwner(orderService.getOrder(orderNumber), session);
    }

    /** The order created by a payment (used right after paying). */
    @GetMapping("/orders/by-payment/{reference}")
    public OrderDto byPayment(@PathVariable("reference") String reference, HttpSession session) {
        return checkOwner(orderService.getOrderByPayment(reference), session);
    }

    /** Receipt Creation: the receipt for an order. */
    @GetMapping("/orders/{orderNumber}/receipt")
    public ReceiptDto receipt(@PathVariable("orderNumber") String orderNumber, HttpSession session) {
        checkOwner(orderService.getOrder(orderNumber), session);
        return orderService.getReceipt(orderNumber);
    }

    /** All orders (staff only). */
    @GetMapping("/admin/orders")
    public List<OrderDto> allOrders(HttpSession session) {
        currentUser.requireAdmin(session);
        return orderService.listAll();
    }

    private OrderDto checkOwner(OrderDto order, HttpSession session) {
        if (!currentUser.canView(session, order.customerId())) {
            throw new AccessDeniedException("You can only view your own orders.");
        }
        return order;
    }
}
