package net.codejava.delivery.dto;

import java.time.LocalDate;
import net.codejava.delivery.model.DeliverySlot;

/** How full a slot is, for the booking form. */
public record SlotAvailabilityDto(LocalDate date, DeliverySlot slot, String label, long booked, int capacity,
                                  boolean available, String reason) {
}
