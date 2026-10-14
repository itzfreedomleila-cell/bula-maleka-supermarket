package net.codejava.delivery.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import net.codejava.delivery.model.Delivery;
import net.codejava.delivery.model.DeliverySlot;
import net.codejava.delivery.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for deliveries. */
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByOrderNumberAndStatusIn(String orderNumber, Collection<DeliveryStatus> statuses);

    long countByDeliveryDateAndSlotAndStatusIn(LocalDate date, DeliverySlot slot, Collection<DeliveryStatus> statuses);

    List<Delivery> findByDriverIdAndDeliveryDateAndSlotAndStatusIn(Long driverId, LocalDate date, DeliverySlot slot,
                                                                  Collection<DeliveryStatus> statuses);

    List<Delivery> findByDriverIdAndDeliveryDateGreaterThanEqualAndStatusIn(Long driverId, LocalDate from,
                                                                           Collection<DeliveryStatus> statuses);

    List<Delivery> findByCustomerId(String customerId);

    List<Delivery> findByCustomerIdAndStatusIn(String customerId, Collection<DeliveryStatus> statuses);

    List<Delivery> findByDeliveryDate(LocalDate date);

    List<Delivery> findByDeliveryDateGreaterThanEqual(LocalDate from);
}
