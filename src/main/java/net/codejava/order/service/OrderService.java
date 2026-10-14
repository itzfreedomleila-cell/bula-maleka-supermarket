package net.codejava.order.service;

import java.util.List;
import net.codejava.order.dto.OrderDto;
import net.codejava.order.dto.ReceiptDto;

/** Module 6 - Order Processing. */
public interface OrderService {

    OrderDto getOrder(String orderNumber);

    OrderDto getOrderByPayment(String paymentReference);

    List<OrderDto> listForCustomer(String customerId);

    List<OrderDto> listAll();

    /** Receipt for a confirmed order. */
    ReceiptDto getReceipt(String orderNumber);

    /** Called by Module 9 when a delivery is booked, cancelled or completed. */
    void markDeliveryScheduled(String orderNumber);

    void markDeliveryCancelled(String orderNumber);

    void markDelivered(String orderNumber);
}
