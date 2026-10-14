package net.codejava.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductSearchCriteria;
import net.codejava.catalog.dto.ProductSort;
import net.codejava.catalog.model.Category;
import net.codejava.catalog.model.Product;
import net.codejava.catalog.repository.CategoryRepository;
import net.codejava.catalog.repository.ProductRepository;
import net.codejava.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

/** Tests for Advanced Search and Filtering (Module 2). */
@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    private CatalogServiceImpl service;
    private Product rice;
    private Product coconut;
    private Product tuna;

    @BeforeEach
    void setUp() {
        service = new CatalogServiceImpl(productRepository, categoryRepository);
        Category pantry = new Category("Pantry", null);
        rice = new Product("Long Grain Rice 10kg", "Family-size bag of white rice", pantry, new BigDecimal("24.95"), 30, null);
        coconut = new Product("Coconut Cream 400ml", "Rich cream for lolo and curries", pantry, new BigDecimal("2.95"), 80, null);
        tuna = new Product("Tinned Mackerel 425g", "Great with rice or cassava", new Category("Tinned Food", null),
                new BigDecimal("3.75"), 70, null);
    }

    @Test
    void keyword_matchesNameBeforeDescription() {
        when(productRepository.filter(isNull(), isNull(), isNull(), anyBoolean(), any(Sort.class)))
                .thenReturn(List.of(coconut, rice, tuna));

        List<ProductDto> result = service.search(new ProductSearchCriteria("rice", null, null, null, false,
                ProductSort.RELEVANCE));

        assertEquals(2, result.size());
        assertEquals("Long Grain Rice 10kg", result.get(0).name()); // name match ranks first
        assertEquals("Tinned Mackerel 425g", result.get(1).name()); // description match
    }

    @Test
    void everyWordMustMatch() {
        when(productRepository.filter(isNull(), isNull(), isNull(), anyBoolean(), any(Sort.class)))
                .thenReturn(List.of(coconut, rice, tuna));

        List<ProductDto> result = service.search(new ProductSearchCriteria("coconut curries", null, null, null,
                false, ProductSort.RELEVANCE));

        assertEquals(1, result.size());
    }

    @Test
    void minAboveMax_isRejected() {
        assertThrows(ValidationException.class, () -> service.search(new ProductSearchCriteria(null, null,
                new BigDecimal("20"), new BigDecimal("5"), false, ProductSort.NAME)));
    }

    @Test
    void splitWords_ignoresPunctuationAndCase() {
        assertEquals(List.of("fresh", "milk", "1l"), CatalogServiceImpl.splitWords("  Fresh, MILK 1L! "));
    }
}
