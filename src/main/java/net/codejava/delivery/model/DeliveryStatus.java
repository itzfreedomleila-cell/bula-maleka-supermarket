package net.codejava.delivery.model;

/** Stages of a delivery booking. */
public enum DeliveryStatus {
    /** Booked by the customer, no driver yet. */
    SCHEDULED,
    /** A driver has been assigned. */
    ASSIGNED,
    /** Staff confirmed the schedule; the customer can rely on it. */
    CONFIRMED,
    DELIVERED,
    CANCELLED;

    /** @return true while the booking still takes up a slot */
    public boolean isActive() {
        return this == SCHEDULED || this == ASSIGNED || this == CONFIRMED;
    }
}
