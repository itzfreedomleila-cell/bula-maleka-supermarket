package net.codejava.delivery.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.codejava.delivery.config.DeliverySettings;
import net.codejava.delivery.model.Delivery;
import net.codejava.delivery.model.DeliverySlot;
import net.codejava.delivery.model.Driver;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Delivery Conflict Detection. Returns every clash it finds, in plain words,
 * so staff and customers know exactly why a booking or assignment is refused.
 */
@Component
public class DeliveryConflictDetector {

    private final DeliverySettings settings;
    private final Clock clock;

    @Autowired
    public DeliveryConflictDetector(DeliverySettings settings) {
        this(settings, Clock.systemDefaultZone());
    }

    /** Used by tests to fix the current time. */
    DeliveryConflictDetector(DeliverySettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /** Conflicts when a customer books a slot. */
    public List<String> scheduleConflicts(LocalDate date, DeliverySlot slot, long bookedInSlot,
                                          boolean orderAlreadyBooked) {
        List<String> conflicts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        if (orderAlreadyBooked) {
            conflicts.add("This order already has a delivery booked. Cancel it first to choose another time.");
        }
        if (date.isBefore(today)) {
            conflicts.add("Choose today or a later date.");
        } else if (date.isAfter(today.plusDays(settings.getBookingDaysAhead()))) {
            conflicts.add("Deliveries can be booked up to " + settings.getBookingDaysAhead() + " days ahead.");
        } else if (slot.startOn(date).isBefore(now.plusHours(settings.getMinimumLeadHours()))) {
            conflicts.add("This slot starts too soon. Choose a slot at least "
                    + settings.getMinimumLeadHours() + " hours from now.");
        }
        if (bookedInSlot >= settings.getMaxOrdersPerSlot()) {
            conflicts.add("The " + slot.getLabel() + " slot on " + date + " is fully booked. Please choose another time.");
        }
        return conflicts;
    }

    /**
     * Conflicts when staff assign a driver.
     *
     * @param otherStops the driver's other active deliveries in the same slot
     * @param routeFits  whether the driver's route, with this stop added, ends within the slot
     * @param routeEnd   when that route's last stop would be reached
     */
    public List<String> assignmentConflicts(Delivery delivery, Driver driver, List<Delivery> otherStops,
                                            boolean routeFits, LocalDateTime routeEnd) {
        List<String> conflicts = new ArrayList<>();
        if (!delivery.getStatus().isActive()) {
            conflicts.add("This delivery is " + delivery.getStatus().name().toLowerCase() + ".");
            return conflicts;
        }
        if (delivery.getDriver() != null) {
            conflicts.add(Objects.equals(delivery.getDriver().getId(), driver.getId())
                    ? driver.getName() + " is already assigned to this delivery."
                    : "Already assigned to " + delivery.getDriver().getName() + ". Remove that driver first.");
        }
        if (!driver.isActive()) {
            conflicts.add(driver.getName() + " is not available for deliveries.");
        }
        if (delivery.getSlotEnd().isBefore(LocalDateTime.now(clock))) {
            conflicts.add("This delivery slot has already finished.");
        }
        if (otherStops.size() >= settings.getMaxStopsPerDriverSlot()) {
            conflicts.add(driver.getName() + " already has " + otherStops.size() + " deliveries in this slot (limit "
                    + settings.getMaxStopsPerDriverSlot() + ").");
        }
        if (!routeFits) {
            conflicts.add(driver.getName() + "'s route would reach the last stop at " + routeEnd.toLocalTime()
                    + ", after the slot ends at " + delivery.getSlot().getEnd() + ".");
        }
        return conflicts;
    }
}
