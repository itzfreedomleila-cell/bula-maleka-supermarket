package net.codejava.catalog.dto;

import java.math.BigDecimal;

/** JSON body for Product Addition and Product Info Edit. */
public record ProductRequest(
        String name,
        String description,
        Long categoryId,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl) {
}
