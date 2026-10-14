package net.codejava.catalog.repository;

import java.util.List;
import java.util.Optional;
import net.codejava.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for categories. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findByNameIgnoreCase(String name);
}
