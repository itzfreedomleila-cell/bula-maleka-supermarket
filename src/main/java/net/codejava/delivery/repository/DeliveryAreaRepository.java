package net.codejava.delivery.repository;

import java.util.List;
import net.codejava.delivery.model.DeliveryArea;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for delivery areas. */
public interface DeliveryAreaRepository extends JpaRepository<DeliveryArea, Long> {

    List<DeliveryArea> findByActiveTrueOrderByNameAsc();
}
