package net.codejava.catalog.dto;

import org.springframework.data.domain.Sort;

/** Sort orders offered in the shop. */
public enum ProductSort {
    /** Best keyword match first (falls back to name when there is no keyword). */
    RELEVANCE(Sort.by("name")),
    NAME(Sort.by("name")),
    PRICE_LOW_HIGH(Sort.by("price").and(Sort.by("name"))),
    PRICE_HIGH_LOW(Sort.by(Sort.Direction.DESC, "price").and(Sort.by("name"))),
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt"));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
