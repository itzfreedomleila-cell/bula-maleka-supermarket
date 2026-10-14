package net.codejava.delivery.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** The two-hour delivery windows customers can book. */
public enum DeliverySlot {
    MORNING(LocalTime.of(9, 0), LocalTime.of(11, 0)),
    MIDDAY(LocalTime.of(11, 0), LocalTime.of(13, 0)),
    AFTERNOON(LocalTime.of(13, 0), LocalTime.of(15, 0)),
    LATE_AFTERNOON(LocalTime.of(15, 0), LocalTime.of(17, 0)),
    EVENING(LocalTime.of(17, 0), LocalTime.of(19, 0));

    private final LocalTime start;
    private final LocalTime end;

    DeliverySlot(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public LocalDateTime startOn(LocalDate date) {
        return date.atTime(start);
    }

    public LocalDateTime endOn(LocalDate date) {
        return date.atTime(end);
    }

    /** @return e.g. "9:00 - 11:00" */
    public String getLabel() {
        return start + " - " + end;
    }
}
