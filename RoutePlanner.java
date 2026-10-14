package net.codejava.delivery.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import net.codejava.delivery.config.DeliverySettings;
import net.codejava.delivery.model.GeoPoint;
import org.springframework.stereotype.Component;

/**
 * Route Scheduling and Estimated Delivery Time.
 * <p>
 * Orders a driver's stops with the nearest-neighbour rule: start at the store,
 * always drive to the closest stop not yet visited. Travel time uses the road
 * distance (straight line x road factor) at the average speed, plus a fixed
 * hand-over time at each stop.
 */
@Component
public class RoutePlanner {

    /** A stop to plan. */
    public record Stop(Long deliveryId, String label, GeoPoint location) {
    }

    /** A planned stop with its position and arrival time. */
    public record PlannedStop(Long deliveryId, String label, int sequence, double legKm, int legMinutes,
                              LocalDateTime eta) {
    }

    /** The whole route. */
    public record RoutePlan(List<PlannedStop> stops, double totalKm, int totalMinutes, LocalDateTime finishTime,
                            boolean fitsInSlot) {
    }

    private final DeliverySettings settings;

    public RoutePlanner(DeliverySettings settings) {
        this.settings = settings;
    }

    /**
     * @param stops     the deliveries in one driver's slot
     * @param slotStart when the driver leaves the store
     * @param slotEnd   last allowed arrival time
     */
    public RoutePlan plan(List<Stop> stops, LocalDateTime slotStart, LocalDateTime slotEnd) {
        List<Stop> remaining = new ArrayList<>(stops);
        List<PlannedStop> planned = new ArrayList<>();
        GeoPoint position = settings.getStore();
        LocalDateTime clock = slotStart;
        double totalKm = 0;
        boolean fits = true;

        while (!remaining.isEmpty()) {
            Stop next = remaining.get(0);
            double nextKm = roadKm(position, next.location());
            for (Stop candidate : remaining) {
                double km = roadKm(position, candidate.location());
                if (km < nextKm) {
                    next = candidate;
                    nextKm = km;
                }
            }
            remaining.remove(next);
            int legMinutes = travelMinutes(nextKm);
            LocalDateTime eta = clock.plusMinutes(legMinutes);
            if (eta.isAfter(slotEnd)) {
                fits = false;
            }
            planned.add(new PlannedStop(next.deliveryId(), next.label(), planned.size() + 1,
                    round1(nextKm), legMinutes, eta));
            totalKm += nextKm;
            clock = eta.plusMinutes(settings.getMinutesPerStop());
            position = next.location();
        }
        int totalMinutes = (int) java.time.Duration.between(slotStart, clock).toMinutes();
        return new RoutePlan(planned, round1(totalKm), totalMinutes, clock, fits);
    }

    /** Estimated Delivery Time before a driver is assigned: straight from the store. */
    public LocalDateTime directEstimate(GeoPoint destination, LocalDateTime slotStart) {
        return slotStart.plusMinutes(travelMinutes(roadKm(settings.getStore(), destination)));
    }

    double roadKm(GeoPoint from, GeoPoint to) {
        return from.distanceKmTo(to) * settings.getRoadFactor();
    }

    int travelMinutes(double km) {
        return (int) Math.ceil(km / settings.getAverageSpeedKmh() * 60);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
