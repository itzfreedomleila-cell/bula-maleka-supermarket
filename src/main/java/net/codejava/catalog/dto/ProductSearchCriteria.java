package net.codejava.catalog.dto;

import java.math.BigDecimal;

/**
 * Advanced Search and Filtering options. Any value left null is not used.
 *
 * @param keyword     words to look for in the name, description and aisle
 * @param categoryId  only this aisle
 * @param minPrice    lowest price
 * @param maxPrice    highest price
 * @param inStockOnly hide sold-out products
 * @param sort        sort order
 */
public record ProductSearchCriteria(
        String keyword,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStockOnly,
        ProductSort sort) {
}
