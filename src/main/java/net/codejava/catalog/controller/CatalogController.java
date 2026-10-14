package net.codejava.catalog.controller;

import java.math.BigDecimal;
import java.util.List;
import net.codejava.catalog.dto.CategoryDto;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductSearchCriteria;
import net.codejava.catalog.dto.ProductSort;
import net.codejava.catalog.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for Module 2 - Product Catalogue (public, no login needed). */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Product List, or Advanced Search and Filtering when parameters are given. */
    @GetMapping("/products")
    public List<ProductDto> products(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "inStock", defaultValue = "false") boolean inStockOnly,
            @RequestParam(value = "sort", defaultValue = "RELEVANCE") ProductSort sort) {
        return catalogService.search(new ProductSearchCriteria(keyword, categoryId, minPrice, maxPrice, inStockOnly, sort));
    }

    /** Product Description, Price and Photo. */
    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable("id") Long id) {
        return catalogService.getProduct(id);
    }

    /** Product Categories. */
    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalogService.listCategories();
    }
}
