package net.codejava.delivery.dto;

import java.time.LocalDate;
import net.codejava.delivery.model.DeliverySlot;

/** JSON body for Schedule Delivery Order. */
public record ScheduleDeliveryRequest(String orderNumber, Long areaId, String address, String contactPhone,
                                      LocalDate date, DeliverySlot slot) {
}
