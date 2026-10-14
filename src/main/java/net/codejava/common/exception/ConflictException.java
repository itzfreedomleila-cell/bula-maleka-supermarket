package net.codejava.common.exception;

import java.util.List;

/** The request clashes with existing data, e.g. a delivery conflict (HTTP 409). */
public class ConflictException extends RuntimeException {

    private final List<String> conflicts;

    public ConflictException(List<String> conflicts) {
        super(conflicts.size() == 1 ? conflicts.get(0) : "This clashes with existing bookings.");
        this.conflicts = List.copyOf(conflicts);
    }

    public ConflictException(String conflict) {
        this(List.of(conflict));
    }

    public List<String> getConflicts() {
        return conflicts;
    }
}
