package net.codejava.delivery.config;

import net.codejava.delivery.model.GeoPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Delivery rules from application.properties (nothing hard-coded). */
@Component
public class DeliverySettings {

    private final int maxOrdersPerSlot;
    private final int maxStopsPerDriverSlot;
    private final double averageSpeedKmh;
    private final int minutesPerStop;
    private final double roadFactor;
    private final GeoPoint store;
    private final int bookingDaysAhead;
    private final int minimumLeadHours;
    private final int cancelCutoffHours;

    public DeliverySettings(
            @Value("${delivery.max-orders-per-slot:12}") int maxOrdersPerSlot,
            @Value("${delivery.max-stops-per-driver-slot:4}") int maxStopsPerDriverSlot,
            @Value("${delivery.average-speed-kmh:30}") double averageSpeedKmh,
            @Value("${delivery.minutes-per-stop:10}") int minutesPerStop,
            @Value("${delivery.road-factor:1.3}") double roadFactor,
            @Value("${delivery.store-latitude:-18.1416}") double storeLatitude,
            @Value("${delivery.store-longitude:178.4419}") double storeLongitude,
            @Value("${delivery.booking-days-ahead:14}") int bookingDaysAhead,
            @Value("${delivery.minimum-lead-hours:2}") int minimumLeadHours,
            @Value("${delivery.cancel-cutoff-hours:2}") int cancelCutoffHours) {
        this.maxOrdersPerSlot = maxOrdersPerSlot;
        this.maxStopsPerDriverSlot = maxStopsPerDriverSlot;
        this.averageSpeedKmh = averageSpeedKmh;
        this.minutesPerStop = minutesPerStop;
        this.roadFactor = roadFactor;
        this.store = new GeoPoint(storeLatitude, storeLongitude);
        this.bookingDaysAhead = bookingDaysAhead;
        this.minimumLeadHours = minimumLeadHours;
        this.cancelCutoffHours = cancelCutoffHours;
    }

    /** Total bookings allowed in one slot across all drivers. */
    public int getMaxOrdersPerSlot() {
        return maxOrdersPerSlot;
    }

    /** Most stops one driver can do in one slot. */
    public int getMaxStopsPerDriverSlot() {
        return maxStopsPerDriverSlot;
    }

    public double getAverageSpeedKmh() {
        return averageSpeedKmh;
    }

    /** Time spent at each address handing over the groceries. */
    public int getMinutesPerStop() {
        return minutesPerStop;
    }

    /** Roads are longer than a straight line; 1.3 = 30% longer. */
    public double getRoadFactor() {
        return roadFactor;
    }

    /** Where every route starts. */
    public GeoPoint getStore() {
        return store;
    }

    public int getBookingDaysAhead() {
        return bookingDaysAhead;
    }

    /** A slot must start at least this many hours after booking. */
    public int getMinimumLeadHours() {
        return minimumLeadHours;
    }

    /** Customers can cancel online until this many hours before the slot. */
    public int getCancelCutoffHours() {
        return cancelCutoffHours;
    }
}
