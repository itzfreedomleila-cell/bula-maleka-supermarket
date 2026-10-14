package net.codejava.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import net.codejava.delivery.config.DeliverySettings;
import net.codejava.delivery.model.GeoPoint;
import org.junit.jupiter.api.Test;

/** Tests for Route Scheduling and Estimated Delivery Time (Module 9). */
class RoutePlannerTest {

    private final RoutePlanner planner = new RoutePlanner(new DeliverySettings(
            12, 4, 30, 10, 1.3, -18.1416, 178.4419, 14, 2, 2));

    private final LocalDateTime start = LocalDateTime.of(2026, 10, 9, 9, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 10, 9, 11, 0);

    private final RoutePlanner.Stop toorak = new RoutePlanner.Stop(1L, "Toorak", new GeoPoint(-18.1355, 178.4445));
    private final RoutePlanner.Stop nabua = new RoutePlanner.Stop(2L, "Nabua", new GeoPoint(-18.1180, 178.4680));
    private final RoutePlanner.Stop nausori = new RoutePlanner.Stop(3L, "Nausori", new GeoPoint(-18.0310, 178.5310));

    @Test
    void nearestStopIsVisitedFirst() {
        RoutePlanner.RoutePlan plan = planner.plan(List.of(nausori, nabua, toorak), start, end);
        assertEquals(List.of(1L, 2L, 3L), plan.stops().stream().map(RoutePlanner.PlannedStop::deliveryId).toList());
    }

    @Test
    void arrivalTimesIncludeTravelAndHandOverTime() {
        RoutePlanner.RoutePlan plan = planner.plan(List.of(toorak, nabua), start, end);
        RoutePlanner.PlannedStop first = plan.stops().get(0);
        RoutePlanner.PlannedStop second = plan.stops().get(1);
        assertEquals(start.plusMinutes(first.legMinutes()), first.eta());
        assertEquals(first.eta().plusMinutes(10 + second.legMinutes()), second.eta());
        assertTrue(plan.fitsInSlot());
    }

    @Test
    void routeTooLongForSlot_isFlagged() {
        LocalDateTime shortEnd = start.plusMinutes(15);
        assertFalse(planner.plan(List.of(nausori), start, shortEnd).fitsInSlot());
    }
}
