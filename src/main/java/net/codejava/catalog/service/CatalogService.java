package net.codejava.catalog.service;

import java.util.List;
import net.codejava.catalog.dto.CategoryDto;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductSearchCriteria;

/** Module 2 - Product Catalogue (read-only, for customers). */
public interface CatalogService {

    /** Product List. */
    List<ProductDto> listProducts();

    /** Product Categories. */
    List<CategoryDto> listCategories();

    /** Product Description, Price and Photo for one product. */
    ProductDto getProduct(Long id);

    /** Advanced Search and Filtering. */
    List<ProductDto> search(ProductSearchCriteria criteria);
}
