package net.codejava.delivery.dto;

import net.codejava.delivery.model.Driver;

/** A driver as shown to staff. */
public record DriverDto(Long id, String name, String phone, String vehicle, boolean active, long upcomingDeliveries) {

    public static DriverDto from(Driver d, long upcoming) {
        return new DriverDto(d.getId(), d.getName(), d.getPhone(), d.getVehicle(), d.isActive(), upcoming);
    }
}
