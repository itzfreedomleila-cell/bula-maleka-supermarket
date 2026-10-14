package net.codejava.delivery.dto;

/** JSON body for cancelling a delivery. */
public record CancelRequest(String reason) {
}
