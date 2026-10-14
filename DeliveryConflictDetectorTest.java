package net.codejava.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import net.codejava.delivery.config.DeliverySettings;
import net.codejava.delivery.model.Delivery;
import net.codejava.delivery.model.DeliveryArea;
import net.codejava.delivery.model.DeliverySlot;
import net.codejava.delivery.model.Driver;
import org.junit.jupiter.api.Test;

/** Tests for Delivery Conflict Detection (Module 9). Today is fixed to 8 Oct 2026, 08:00. */
class DeliveryConflictDetectorTest {

    private static final ZoneId FIJI = ZoneId.of("Pacific/Fiji");
    private final DeliveryConflictDetector detector = new DeliveryConflictDetector(
            new DeliverySettings(2, 2, 30, 10, 1.3, -18.1416, 178.4419, 14, 2, 2),
            Clock.fixed(LocalDateTime.of(2026, 10, 8, 8, 0).atZone(FIJI).toInstant(), FIJI));

    private final LocalDate tomorrow = LocalDate.of(2026, 10, 9);
    private final DeliveryArea toorak = new DeliveryArea("Toorak", -18.1355, 178.4445);

    private Delivery delivery() {
        return new Delivery("ORD-1", "7", toorak, "12 Gordon St", "9123456", tomorrow, DeliverySlot.MORNING,
                tomorrow.atTime(9, 5));
    }

    @Test
    void freeSlotTomorrow_hasNoConflicts() {
        assertTrue(detector.scheduleConflicts(tomorrow, DeliverySlot.MORNING, 0, false).isEmpty());
    }

    @Test
    void fullSlot_pastDate_andTooSoon_areDetected() {
        assertEquals(1, detector.scheduleConflicts(tomorrow, DeliverySlot.MORNING, 2, false).size());
        assertEquals(1, detector.scheduleConflicts(LocalDate.of(2026, 10, 7), DeliverySlot.MORNING, 0, false).size());
        // 9:00 today is only 1 hour away (minimum is 2 hours)
        assertEquals(1, detector.scheduleConflicts(LocalDate.of(2026, 10, 8), DeliverySlot.MORNING, 0, false).size());
    }

    @Test
    void orderBookedTwice_isDetected() {
        assertEquals(1, detector.scheduleConflicts(tomorrow, DeliverySlot.MIDDAY, 0, true).size());
    }

    @Test
    void busyDriver_inactiveDriver_andLongRoute_areDetected() {
        Driver josefa = new Driver("Josefa Vula", "9201001", "Van");
        josefa.setId(1L);
        josefa.setActive(false);
        List<String> conflicts = detector.assignmentConflicts(delivery(), josefa,
                List.of(delivery(), delivery()), false, tomorrow.atTime(11, 20));
        assertEquals(3, conflicts.size());
    }

    @Test
    void driverWithRoom_hasNoConflicts() {
        Driver mere = new Driver("Mere Tabua", "9201004", "Van");
        mere.setId(4L);
        assertTrue(detector.assignmentConflicts(delivery(), mere, List.of(), true, tomorrow.atTime(9, 10)).isEmpty());
    }
}
