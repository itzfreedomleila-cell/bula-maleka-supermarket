package net.codejava.delivery.dto;

import java.util.List;

/** Result of checking a driver assignment before saving it. */
public record ConflictCheckDto(boolean ok, List<String> conflicts, RouteDto projectedRoute) {
}
