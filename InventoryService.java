package net.codejava.catalog.service;

import java.util.List;
import net.codejava.catalog.dto.CategoryDto;
import net.codejava.catalog.dto.CategoryRequest;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductRequest;
import org.springframework.web.multipart.MultipartFile;

/** Module 3 - Inventory Management (staff only). */
public interface InventoryService {

    /** All products, including removed ones. */
    List<ProductDto> listAllProducts();

    /** Product Addition. */
    ProductDto addProduct(ProductRequest request);

    /** Product Info Edit. */
    ProductDto editProduct(Long id, ProductRequest request);

    /** Product Removal (hidden from the shop, kept for order history). */
    ProductDto removeProduct(Long id);

    /** Puts a removed product back on sale. */
    ProductDto restoreProduct(Long id);

    /** Uploads a product photo. */
    ProductDto uploadPhoto(Long id, MultipartFile file);

    /** Adds a product category. */
    CategoryDto addCategory(CategoryRequest request);
}
