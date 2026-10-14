package net.codejava.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductRequest;
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

/** Tests for Product Addition, Info Edit and Removal (Module 3). */
@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    private InventoryServiceImpl service;
    private Category pantry;

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(productRepository, categoryRepository, "build/test-uploads");
        pantry = new Category("Pantry", null);
        pantry.setId(4L);
    }

    @Test
    void addProduct_validDetails_isSaved() {
        when(productRepository.existsByNameIgnoreCaseAndActiveTrue("Taro Leaves (Rourou)")).thenReturn(false);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(pantry));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto saved = service.addProduct(new ProductRequest("Taro Leaves (Rourou)", "Fresh bundle", 4L,
                new BigDecimal("3.5"), 25, null));

        assertEquals(new BigDecimal("3.50"), saved.price());
        assertEquals("Pantry", saved.categoryName());
    }

    @Test
    void addProduct_badDetails_reportsAllProblems() {
        ValidationException ex = assertThrows(ValidationException.class, () -> service.addProduct(
                new ProductRequest("", null, null, new BigDecimal("-1"), -5, "ftp://bad")));
        assertTrue(ex.getErrors().size() >= 4);
    }

    @Test
    void removeProduct_hidesItButKeepsIt() {
        Product rice = new Product("Rice", null, pantry, new BigDecimal("24.95"), 10, null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(rice));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(service.removeProduct(1L).active());
    }
}
