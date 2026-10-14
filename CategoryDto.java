package net.codejava.catalog.dto;

/** A category with how many products it has on sale. */
public record CategoryDto(Long id, String name, String description, long productCount) {
}
