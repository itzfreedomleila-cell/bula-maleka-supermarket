package net.codejava.order.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.codejava.account.model.User;
import net.codejava.account.repository.UserRepository;
import net.codejava.common.exception.NotFoundException;
import net.codejava.order.dto.OrderDto;
import net.codejava.order.dto.ReceiptDto;
import net.codejava.order.model.CustomerOrder;
import net.codejava.order.model.OrderItem;
import net.codejava.order.repository.OrderRepository;
import net.codejava.payment.event.PaymentRefundedEvent;
import net.codejava.payment.event.PaymentSucceededEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order Processing. Orders are created automatically when Module 5 reports a
 * successful payment, then confirmed and given a receipt in the same step.
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final String storeName;
    private final String storeAddress;
    private final String storePhone;

    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository,
                            @Value("${app.store.name:Bula Maleka Supermarket}") String storeName,
                            @Value("${app.store.address:Suva, Fiji}") String storeAddress,
                            @Value("${app.store.phone:}") String storePhone) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.storeName = storeName;
        this.storeAddress = storeAddress;
        this.storePhone = storePhone;
    }

    /**
     * Order Creation, Order Confirmation and Receipt Creation, triggered by a
     * successful payment. Safe to receive twice: a second event is ignored.
     */
    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (orderRepository.findByPaymentReference(event.paymentReference()).isPresent()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CustomerOrder order = new CustomerOrder(generateCode("ORD"), event.customerId(),
                event.paymentReference(), event.methodLabel(), event.subtotal(), event.deliveryFee(),
                event.surcharge(), event.total());
        event.lines().forEach(l -> order.addItem(
                new OrderItem(l.productId(), l.productName(), l.quantity(), l.unitPrice(), l.subtotal())));
        order.confirm(now);
        order.issueReceipt(generateCode("RCP"), now);
        orderRepository.save(order);
    }

    /** Keeps the order in step with refunds. */
    @EventListener
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        orderRepository.findByPaymentReference(event.paymentReference())
                .ifPresent(o -> o.recordRefund(event.totalRefunded(), event.fullyRefunded()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(String orderNumber) {
        CustomerOrder order = find(orderNumber);
        return OrderDto.from(order, customerName(order.getCustomerId()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderByPayment(String paymentReference) {
        CustomerOrder order = orderRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new NotFoundException("No order for payment " + paymentReference + "."));
        return OrderDto.from(order, customerName(order.getCustomerId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listForCustomer(String customerId) {
        String name = customerName(customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(o -> OrderDto.from(o, name))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> OrderDto.from(o, customerName(o.getCustomerId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptDto getReceipt(String orderNumber) {
        CustomerOrder o = find(orderNumber);
        if (o.getReceiptNumber() == null) {
            throw new NotFoundException("No receipt has been issued for this order yet.");
        }
        String note = o.getRefundedAmount().signum() > 0
                ? "FJ$" + o.getRefundedAmount() + " of this order has been refunded."
                : "Thank you for shopping with us.";
        return new ReceiptDto(storeName, storeAddress, storePhone, o.getReceiptNumber(), o.getReceiptIssuedAt(),
                o.getOrderNumber(), customerName(o.getCustomerId()),
                OrderDto.from(o, null).items(), o.getSubtotal(), o.getDeliveryFee(), o.getSurcharge(),
                o.getTotal(), o.getRefundedAmount(), o.getPaymentMethod(), o.getPaymentReference(), note);
    }

    @Override
    public void markDeliveryScheduled(String orderNumber) {
        find(orderNumber).markDeliveryScheduled();
    }

    @Override
    public void markDeliveryCancelled(String orderNumber) {
        find(orderNumber).markDeliveryCancelled();
    }

    @Override
    public void markDelivered(String orderNumber) {
        find(orderNumber).markDelivered();
    }

    // ---------------------------------------------------------------

    private CustomerOrder find(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order " + orderNumber + " was not found."));
    }

    private String customerName(String customerId) {
        try {
            return userRepository.findById(Long.valueOf(customerId)).map(User::getFullName).orElse("Customer");
        } catch (NumberFormatException ex) {
            return "Customer";
        }
    }

    /** @return e.g. ORD-20261008-7K3QXA */
    private static String generateCode(String prefix) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return prefix + "-" + LocalDateTime.now().format(DAY) + "-" + code;
    }
}
