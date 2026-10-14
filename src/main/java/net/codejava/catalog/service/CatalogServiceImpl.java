package net.codejava.catalog.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.codejava.catalog.dto.CategoryDto;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductSearchCriteria;
import net.codejava.catalog.dto.ProductSort;
import net.codejava.catalog.model.Product;
import net.codejava.catalog.repository.CategoryRepository;
import net.codejava.catalog.repository.ProductRepository;
import net.codejava.common.exception.NotFoundException;
import net.codejava.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product Catalogue logic. Filters (aisle, price, stock) run in the database;
 * the keyword search then matches every word against the name, description
 * and aisle, and ranks name matches first.
 */
@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CatalogServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<ProductDto> listProducts() {
        return productRepository.findByActiveTrue(ProductSort.NAME.toSort()).stream()
                .map(ProductDto::from)
                .toList();
    }

    @Override
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getDescription(),
                        productRepository.countByCategoryIdAndActiveTrue(c.getId())))
                .toList();
    }

    @Override
    public ProductDto getProduct(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .map(ProductDto::from)
                .orElseThrow(() -> new NotFoundException("This product is not available."));
    }

    @Override
    public List<ProductDto> search(ProductSearchCriteria criteria) {
        BigDecimal min = criteria.minPrice();
        BigDecimal max = criteria.maxPrice();
        if ((min != null && min.signum() < 0) || (max != null && max.signum() < 0)) {
            throw new ValidationException("Prices cannot be negative.");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ValidationException("Minimum price cannot be more than maximum price.");
        }
        ProductSort sort = criteria.sort() == null ? ProductSort.RELEVANCE : criteria.sort();
        List<Product> filtered = productRepository.filter(
                criteria.categoryId(), min, max, criteria.inStockOnly(), sort.toSort());

        List<String> words = splitWords(criteria.keyword());
        if (words.isEmpty()) {
            return filtered.stream().map(ProductDto::from).toList();
        }
        List<Product> matches = new ArrayList<>();
        for (Product product : filtered) {
            if (score(product, words) > 0) {
                matches.add(product);
            }
        }
        if (sort == ProductSort.RELEVANCE) {
            matches.sort(Comparator.comparingInt((Product p) -> score(p, words)).reversed()
                    .thenComparing(Product::getName));
        }
        return matches.stream().map(ProductDto::from).toList();
    }

    /** Lower-case words of at least 2 letters. */
    static List<String> splitWords(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<String> words = new ArrayList<>();
        for (String w : keyword.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (w.length() >= 2) {
                words.add(w);
            }
        }
        return words;
    }

    /**
     * Relevance score: 0 if any word is missing; otherwise 3 points per word
     * found in the name, 1 per word found only in the description or aisle.
     */
    static int score(Product product, List<String> words) {
        String name = product.getName().toLowerCase(Locale.ROOT);
        String other = ((product.getDescription() == null ? "" : product.getDescription()) + " "
                + product.getCategoryName()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String word : words) {
            if (name.contains(word)) {
                score += 3;
            } else if (other.contains(word)) {
                score += 1;
            } else {
                return 0;
            }
        }
        return score;
    }
}
