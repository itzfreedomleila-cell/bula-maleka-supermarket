package net.codejava.delivery.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import net.codejava.delivery.model.Delivery;
import net.codejava.delivery.model.DeliverySlot;
import net.codejava.delivery.model.DeliveryStatus;

/** A delivery booking as shown to the customer or staff. */
public record DeliveryDto(
        Long id,
        String orderNumber,
        String customerId,
        String customerName,
        String areaName,
        String address,
        String contactPhone,
        LocalDate date,
        DeliverySlot slot,
        String slotLabel,
        DeliveryStatus status,
        Long driverId,
        String driverName,
        String driverPhone,
        Integer routeSequence,
        LocalDateTime estimatedArrival,
        String cancelReason,
        boolean cancellable) {

    public static DeliveryDto from(Delivery d, String customerName, boolean cancellable) {
        return new DeliveryDto(d.getId(), d.getOrderNumber(), d.getCustomerId(), customerName,
                d.getArea().getName(), d.getAddress(), d.getContactPhone(), d.getDeliveryDate(), d.getSlot(),
                d.getSlot().getLabel(), d.getStatus(),
                d.getDriver() == null ? null : d.getDriver().getId(),
                d.getDriver() == null ? null : d.getDriver().getName(),
                d.getDriver() == null ? null : d.getDriver().getPhone(),
                d.getRouteSequence(), d.getEstimatedArrival(), d.getCancelReason(), cancellable);
    }
}
