package net.codejava.catalog.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.codejava.catalog.dto.CategoryDto;
import net.codejava.catalog.dto.CategoryRequest;
import net.codejava.catalog.dto.ProductDto;
import net.codejava.catalog.dto.ProductRequest;
import net.codejava.catalog.model.Category;
import net.codejava.catalog.model.Product;
import net.codejava.catalog.repository.CategoryRepository;
import net.codejava.catalog.repository.ProductRepository;
import net.codejava.common.exception.NotFoundException;
import net.codejava.common.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Inventory Management: add, edit and remove products, and upload photos.
 */
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final BigDecimal MAX_PRICE = new BigDecimal("10000.00");
    private static final int MAX_STOCK = 100_000;
    private static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;
    /** Allowed photo types and the file extension used for each. */
    private static final Map<String, String> PHOTO_TYPES = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final Path uploadDir;

    public InventoryServiceImpl(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> listAllProducts() {
        return productRepository.findAllByOrderByActiveDescNameAsc().stream().map(ProductDto::from).toList();
    }

    @Override
    public ProductDto addProduct(ProductRequest request) {
        Category category = validate(request, null);
        Product product = new Product(request.name().trim(), clean(request.description()), category,
                request.price().setScale(2, java.math.RoundingMode.HALF_UP), request.stockQuantity(),
                clean(request.imageUrl()));
        return ProductDto.from(productRepository.save(product));
    }

    @Override
    public ProductDto editProduct(Long id, ProductRequest request) {
        Product product = find(id);
        Category category = validate(request, id);
        product.updateDetails(request.name().trim(), clean(request.description()), category,
                request.price().setScale(2, java.math.RoundingMode.HALF_UP), request.stockQuantity(),
                clean(request.imageUrl()));
        return ProductDto.from(productRepository.save(product));
    }

    @Override
    public ProductDto removeProduct(Long id) {
        Product product = find(id);
        product.remove();
        return ProductDto.from(productRepository.save(product));
    }

    @Override
    public ProductDto restoreProduct(Long id) {
        Product product = find(id);
        if (productRepository.existsByNameIgnoreCaseAndActiveTrueAndIdNot(product.getName(), id)) {
            throw new ValidationException("Another product on sale already has this name.");
        }
        product.restore();
        return ProductDto.from(productRepository.save(product));
    }

    @Override
    public ProductDto uploadPhoto(Long id, MultipartFile file) {
        Product product = find(id);
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Choose a photo to upload.");
        }
        String extension = PHOTO_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ValidationException("Photo must be a JPG, PNG or WEBP image.");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new ValidationException("Photo is too large. The maximum is 2 MB.");
        }
        // The file name is made by the server, never taken from the upload, so it is always safe.
        String fileName = "product-" + id + "-" + System.currentTimeMillis() + extension;
        Path folder = uploadDir.resolve("products");
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(folder);
            Files.copy(in, folder.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ValidationException("The photo could not be saved. Please try again.");
        }
        product.setImageUrl("/uploads/products/" + fileName);
        return ProductDto.from(productRepository.save(product));
    }

    @Override
    public CategoryDto addCategory(CategoryRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.length() < 2 || name.length() > 50) {
            throw new ValidationException("Category name must be 2 to 50 characters.");
        }
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ValidationException("This category already exists.");
        }
        Category saved = categoryRepository.save(new Category(name, clean(request.description())));
        return new CategoryDto(saved.getId(), saved.getName(), saved.getDescription(), 0);
    }

    // ---------------------------------------------------------------

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " was not found."));
    }

    /** Checks every field and returns the chosen category. */
    private Category validate(ProductRequest r, Long editingId) {
        List<String> errors = new ArrayList<>();
        String name = r.name() == null ? "" : r.name().trim();
        if (name.length() < 2 || name.length() > 100) {
            errors.add("Product name must be 2 to 100 characters.");
        } else if (editingId == null
                ? productRepository.existsByNameIgnoreCaseAndActiveTrue(name)
                : productRepository.existsByNameIgnoreCaseAndActiveTrueAndIdNot(name, editingId)) {
            errors.add("Another product on sale already has this name.");
        }
        if (r.description() != null && r.description().length() > 1000) {
            errors.add("Description must be 1000 characters or fewer.");
        }
        if (r.price() == null || r.price().signum() <= 0 || r.price().compareTo(MAX_PRICE) > 0) {
            errors.add("Price must be more than FJ$0 and at most FJ$" + MAX_PRICE + ".");
        }
        if (r.stockQuantity() == null || r.stockQuantity() < 0 || r.stockQuantity() > MAX_STOCK) {
            errors.add("Stock must be a whole number from 0 to " + MAX_STOCK + ".");
        }
        String image = clean(r.imageUrl());
        if (image != null && !image.startsWith("/") && !image.startsWith("https://")) {
            errors.add("Photo link must start with / or https://");
        }
        Category category = null;
        if (r.categoryId() == null) {
            errors.add("Choose a category.");
        } else {
            category = categoryRepository.findById(r.categoryId()).orElse(null);
            if (category == null) {
                errors.add("The chosen category does not exist.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return category;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
