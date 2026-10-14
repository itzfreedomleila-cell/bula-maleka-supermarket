package net.codejava.delivery.dto;

/** JSON body for adding a driver. */
public record DriverRequest(String name, String phone, String vehicle) {
}
