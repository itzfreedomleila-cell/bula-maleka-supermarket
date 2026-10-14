package net.codejava.catalog;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.codejava.catalog.model.Category;
import net.codejava.catalog.model.Product;
import net.codejava.catalog.repository.CategoryRepository;
import net.codejava.catalog.repository.ProductRepository;
import net.codejava.common.seed.SeedFileReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Loads the starter categories and products from seed/categories.psv and
 * seed/products.psv.
 * <p>
 * Normal use (catalog.sync-from-file=false): the files are loaded once. After
 * that the seeder does nothing, so edits made on the Inventory page are kept.
 * <p>
 * While you are editing the files (catalog.sync-from-file=true): every start
 * makes the shop match the files. Products in the file are added or updated,
 * and products that are not in the file are hidden (removed, not deleted, so
 * old orders keep working).
 */
@Component
@Order(1)
public class CatalogSeeder implements CommandLineRunner {

    private static final String DEFAULT_PHOTO = "/images/products/default.svg";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SeedFileReader reader;
    private final boolean syncFromFile;

    public CatalogSeeder(CategoryRepository categoryRepository, ProductRepository productRepository,
                         SeedFileReader reader,
                         @Value("${catalog.sync-from-file:false}") boolean syncFromFile) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.reader = reader;
        this.syncFromFile = syncFromFile;
    }

    @Override
    public void run(String... args) {
        // columns: category | name | description | price | stock | image
        List<String[]> products = reader.read("products.psv");
        if (!syncFromFile && alreadySynced(products)) {
            return;
        }

        for (String[] row : reader.read("categories.psv")) {
            if (categoryRepository.findByNameIgnoreCase(row[0]).isEmpty()) {
                categoryRepository.save(new Category(row[0], row[1]));
            }
        }

        Set<String> seedNames = new HashSet<>();
        for (String[] row : products) {
            Category category = categoryRepository.findByNameIgnoreCase(row[0])
                    .orElseGet(() -> categoryRepository.save(new Category(row[0], null)));
            BigDecimal price = new BigDecimal(row[3]);
            int stock = Integer.parseInt(row[4]);
            seedNames.add(row[1].toLowerCase());

            Product existing = productRepository.findFirstByNameIgnoreCase(row[1]).orElse(null);
            if (existing == null) {
                productRepository.save(new Product(row[1], row[2], category, price, stock, row[5]));
            } else {
                existing.updateDetails(row[1], row[2], category, price, stock, row[5]);
                existing.restore();
                productRepository.save(existing);
            }
        }

        // Hide products that are not in the file any more
        for (Product old : productRepository.findByActiveTrue(Sort.unsorted())) {
            if (!seedNames.contains(old.getName().toLowerCase())) {
                old.remove();
                productRepository.save(old);
            }
        }
    }

    /** True when at least one starter product (with its own photo) is already on sale. */
    private boolean alreadySynced(List<String[]> products) {
        for (String[] row : products) {
            if (!DEFAULT_PHOTO.equals(row[5]) && productRepository.existsByImageUrlAndActiveTrue(row[5])) {
                return true;
            }
        }
        return false;
    }
}