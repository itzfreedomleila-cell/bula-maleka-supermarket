package net.codejava.delivery.service;

import java.time.LocalDate;
import java.util.List;
import net.codejava.delivery.dto.AreaDto;
import net.codejava.delivery.dto.ConflictCheckDto;
import net.codejava.delivery.dto.DeliveryDto;
import net.codejava.delivery.dto.DriverDto;
import net.codejava.delivery.dto.DriverRequest;
import net.codejava.delivery.dto.RouteDto;
import net.codejava.delivery.dto.ScheduleDeliveryRequest;
import net.codejava.delivery.dto.SlotAvailabilityDto;
import net.codejava.delivery.model.DeliverySlot;

/** Module 9 - Delivery. */
public interface DeliveryService {

    // ----- Customer -----

    List<AreaDto> listAreas();

    /** How full each slot is on a date. */
    List<SlotAvailabilityDto> slotAvailability(LocalDate date);

    /** Schedule Delivery Order (with Estimated Delivery Time). */
    DeliveryDto schedule(String customerId, ScheduleDeliveryRequest request);

    List<DeliveryDto> listForCustomer(String customerId);

    /** Cancel Schedule (customer, before the cut-off). */
    DeliveryDto cancelByCustomer(String customerId, Long deliveryId);

    // ----- Staff -----

    /** Deliveries on a date, or all upcoming ones if date is null. */
    List<DeliveryDto> listDeliveries(LocalDate date);

    List<DriverDto> listDrivers();

    DriverDto addDriver(DriverRequest request);

    /** Makes a driver unavailable (removing them from upcoming deliveries) or available again. */
    DriverDto setDriverActive(Long driverId, boolean active);

    /** Delivery Conflict Detection: preview an assignment without saving. */
    ConflictCheckDto checkAssignment(Long deliveryId, Long driverId);

    /** Driver Assignment. */
    DeliveryDto assignDriver(Long deliveryId, Long driverId);

    /** Removed Appointed Delivery Driver. */
    DeliveryDto removeDriver(Long deliveryId);

    /** Confirm Delivery Order Schedule. */
    DeliveryDto confirm(Long deliveryId);

    /** Cancel Schedule (staff, any time before delivery). */
    DeliveryDto cancelByStaff(Long deliveryId, String reason);

    DeliveryDto markDelivered(Long deliveryId);

    /** Route Scheduling: a driver's stops in order with arrival times. */
    RouteDto getRoute(Long driverId, LocalDate date, DeliverySlot slot);
}
