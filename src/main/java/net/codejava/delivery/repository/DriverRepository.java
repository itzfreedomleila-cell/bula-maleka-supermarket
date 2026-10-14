package net.codejava.delivery.repository;

import java.util.List;
import net.codejava.delivery.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for drivers. */
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findAllByOrderByActiveDescNameAsc();
}
