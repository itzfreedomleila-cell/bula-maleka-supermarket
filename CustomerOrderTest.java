package net.codejava.order.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import net.codejava.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for Order Creation, Confirmation and Receipt Creation (Module 6). */
class CustomerOrderTest {

    private CustomerOrder order;
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 8, 10, 0);

    @BeforeEach
    void setUp() {
        order = new CustomerOrder("ORD-1", "7", "SPM-1", "VISA ending 4242", new BigDecimal("49.90"),
                new BigDecimal("7.50"), new BigDecimal("0.86"), new BigDecimal("58.26"));
    }

    @Test
    void newOrder_startsAsCreated() {
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void confirm_needsAtLeastOneItem() {
        assertThrows(ValidationException.class, () -> order.confirm(now));
    }

    @Test
    void confirmThenReceipt_followsTheRightOrder() {
        order.addItem(new OrderItem(9L, "Rice", 2, new BigDecimal("24.95"), new BigDecimal("49.90")));
        assertThrows(ValidationException.class, () -> order.issueReceipt("RCP-1", now)); // not confirmed yet
        order.confirm(now);
        order.issueReceipt("RCP-1", now);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals("RCP-1", order.getReceiptNumber());
        assertThrows(ValidationException.class, () -> order.issueReceipt("RCP-2", now)); // only once
    }

    @Test
    void deliveryLifecycle_updatesStatus() {
        order.addItem(new OrderItem(9L, "Rice", 1, new BigDecimal("24.95"), new BigDecimal("24.95")));
        order.confirm(now);
        order.markDeliveryScheduled();
        assertEquals(OrderStatus.DELIVERY_SCHEDULED, order.getStatus());
        order.markDeliveryCancelled();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        order.markDeliveryScheduled();
        order.markDelivered();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }
}
