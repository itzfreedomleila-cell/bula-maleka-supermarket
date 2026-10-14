package net.codejava.catalog.dto;

/** JSON body for adding a category. */
public record CategoryRequest(String name, String description) {
}
