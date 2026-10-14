package net.codejava.catalog.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import net.codejava.catalog.model.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Database access for products. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Product List: everything on sale. */
    List<Product> findByActiveTrue(Sort sort);

    /** Inventory screen: every product, removed ones included. */
    List<Product> findAllByOrderByActiveDescNameAsc();

    long countByCategoryIdAndActiveTrue(Long categoryId);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    /** Used by the catalogue seeder to find a starter product by name, removed or not. */
    Optional<Product> findFirstByNameIgnoreCase(String name);

    /** Used by the catalogue seeder to tell whether the starter products are already loaded. */
    boolean existsByImageUrlAndActiveTrue(String imageUrl);

    boolean existsByNameIgnoreCaseAndActiveTrueAndIdNot(String name, Long id);

    /**
     * Filtering done by the database. Any filter left null is ignored.
     * Keyword matching is done afterwards in CatalogServiceImpl.
     */
    @Query("select p from Product p where p.active = true"
            + " and (:categoryId is null or p.category.id = :categoryId)"
            + " and (:minPrice is null or p.price >= :minPrice)"
            + " and (:maxPrice is null or p.price <= :maxPrice)"
            + " and (:inStockOnly = false or p.stockQuantity > 0)")
    List<Product> filter(@Param("categoryId") Long categoryId,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("inStockOnly") boolean inStockOnly,
                         Sort sort);
}