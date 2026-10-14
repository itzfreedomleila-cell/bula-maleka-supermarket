package net.codejava.catalog.dto;

import java.math.BigDecimal;
import net.codejava.catalog.model.Product;

/** A product as shown in the shop (Product Description, Price and Photo). */
public record ProductDto(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        int stockQuantity,
        boolean inStock,
        String imageUrl,
        boolean active) {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getDescription(),
                p.getCategory() == null ? null : p.getCategory().getId(), p.getCategoryName(),
                p.getPrice(), p.getStockQuantity(), p.getStockQuantity() > 0, p.getImageUrl(), p.isActive());
    }
}
