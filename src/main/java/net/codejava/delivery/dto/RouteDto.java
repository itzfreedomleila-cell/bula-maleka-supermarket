package net.codejava.delivery.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import net.codejava.delivery.model.DeliverySlot;

/** A driver's planned route for one slot (Route Scheduling). */
public record RouteDto(Long driverId, String driverName, LocalDate date, DeliverySlot slot, String slotLabel,
                       List<Stop> stops, double totalKm, int totalMinutes, LocalDateTime finishTime,
                       boolean fitsInSlot) {

    public record Stop(int sequence, Long deliveryId, String orderNumber, String areaName, String address,
                       double legKm, int legMinutes, LocalDateTime eta) {
    }
}
