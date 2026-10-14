package net.codejava.delivery.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.codejava.account.event.AccountDeletedEvent;
import net.codejava.account.model.User;
import net.codejava.account.repository.UserRepository;
import net.codejava.common.exception.AccessDeniedException;
import net.codejava.common.exception.ConflictException;
import net.codejava.common.exception.NotFoundException;
import net.codejava.common.exception.ValidationException;
import net.codejava.delivery.config.DeliverySettings;
import net.codejava.delivery.dto.AreaDto;
import net.codejava.delivery.dto.ConflictCheckDto;
import net.codejava.delivery.dto.DeliveryDto;
import net.codejava.delivery.dto.DriverDto;
import net.codejava.delivery.dto.DriverRequest;
import net.codejava.delivery.dto.RouteDto;
import net.codejava.delivery.dto.ScheduleDeliveryRequest;
import net.codejava.delivery.dto.SlotAvailabilityDto;
import net.codejava.delivery.model.Delivery;
import net.codejava.delivery.model.DeliveryArea;
import net.codejava.delivery.model.DeliverySlot;
import net.codejava.delivery.model.DeliveryStatus;
import net.codejava.delivery.model.Driver;
import net.codejava.delivery.repository.DeliveryAreaRepository;
import net.codejava.delivery.repository.DeliveryRepository;
import net.codejava.delivery.repository.DriverRepository;
import net.codejava.order.dto.OrderDto;
import net.codejava.order.model.OrderStatus;
import net.codejava.order.service.OrderService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivery logic. Whenever a driver's stops change, their route for that slot
 * is planned again so the stop order and arrival times stay correct.
 */
@Service
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private static final Set<DeliveryStatus> ACTIVE =
            EnumSet.of(DeliveryStatus.SCHEDULED, DeliveryStatus.ASSIGNED, DeliveryStatus.CONFIRMED);
    private static final Set<DeliveryStatus> ROUTED = EnumSet.of(DeliveryStatus.ASSIGNED, DeliveryStatus.CONFIRMED);
    /** Sorts by date, then slot in time order, then route position. */
    private static final Comparator<Delivery> BY_TIME = Comparator.comparing(Delivery::getDeliveryDate)
            .thenComparing(d -> d.getSlot().ordinal())
            .thenComparing(d -> d.getRouteSequence() == null ? Integer.MAX_VALUE : d.getRouteSequence());

    private final DeliveryRepository deliveryRepository;
    private final DriverRepository driverRepository;
    private final DeliveryAreaRepository areaRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final RoutePlanner routePlanner;
    private final DeliveryConflictDetector conflictDetector;
    private final DeliverySettings settings;
    private final Clock clock = Clock.systemDefaultZone();

    public DeliveryServiceImpl(DeliveryRepository deliveryRepository, DriverRepository driverRepository,
                               DeliveryAreaRepository areaRepository, UserRepository userRepository,
                               OrderService orderService, RoutePlanner routePlanner,
                               DeliveryConflictDetector conflictDetector, DeliverySettings settings) {
        this.deliveryRepository = deliveryRepository;
        this.driverRepository = driverRepository;
        this.areaRepository = areaRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.routePlanner = routePlanner;
        this.conflictDetector = conflictDetector;
        this.settings = settings;
    }

    // =================================================================
    // Customer
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public List<AreaDto> listAreas() {
        return areaRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(a -> new AreaDto(a.getId(), a.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotAvailabilityDto> slotAvailability(LocalDate date) {
        if (date == null) {
            throw new ValidationException("Choose a date.");
        }
        List<SlotAvailabilityDto> result = new ArrayList<>();
        for (DeliverySlot slot : DeliverySlot.values()) {
            long booked = deliveryRepository.countByDeliveryDateAndSlotAndStatusIn(date, slot, ACTIVE);
            List<String> conflicts = conflictDetector.scheduleConflicts(date, slot, booked, false);
            result.add(new SlotAvailabilityDto(date, slot, slot.getLabel(), booked, settings.getMaxOrdersPerSlot(),
                    conflicts.isEmpty(), conflicts.isEmpty() ? null : conflicts.get(0)));
        }
        return result;
    }

    @Override
    public DeliveryDto schedule(String customerId, ScheduleDeliveryRequest r) {
        List<String> errors = new ArrayList<>();
        String address = r.address() == null ? "" : r.address().trim();
        String phone = r.contactPhone() == null ? "" : r.contactPhone().replaceAll("\\D", "");
        if (r.orderNumber() == null || r.orderNumber().isBlank()) {
            errors.add("Choose the order to deliver.");
        }
        if (r.areaId() == null) {
            errors.add("Choose your area.");
        }
        if (address.length() < 5 || address.length() > 200) {
            errors.add("Enter the street address (5 to 200 characters).");
        }
        if (!phone.matches("\\d{7}")) {
            errors.add("Enter a 7-digit contact phone number.");
        }
        if (r.date() == null || r.slot() == null) {
            errors.add("Choose a delivery date and time slot.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        OrderDto order = orderService.getOrder(r.orderNumber());
        if (!order.customerId().equals(customerId)) {
            throw new AccessDeniedException("You can only book delivery for your own orders.");
        }
        boolean alreadyBooked = !deliveryRepository.findByOrderNumberAndStatusIn(r.orderNumber(), ACTIVE).isEmpty();
        if (!alreadyBooked && order.status() != OrderStatus.CONFIRMED) {
            throw new ValidationException("Order " + order.orderNumber() + " is " + order.status().name().toLowerCase()
                    + " and cannot be booked for delivery.");
        }
        DeliveryArea area = areaRepository.findById(r.areaId())
                .filter(DeliveryArea::isActive)
                .orElseThrow(() -> new ValidationException("We do not deliver to that area."));

        long booked = deliveryRepository.countByDeliveryDateAndSlotAndStatusIn(r.date(), r.slot(), ACTIVE);
        List<String> conflicts = conflictDetector.scheduleConflicts(r.date(), r.slot(), booked, alreadyBooked);
        if (!conflicts.isEmpty()) {
            throw new ConflictException(conflicts);
        }
        Delivery delivery = new Delivery(r.orderNumber(), customerId, area, address, phone, r.date(), r.slot(),
                routePlanner.directEstimate(area.getLocation(), r.slot().startOn(r.date())));
        deliveryRepository.save(delivery);
        orderService.markDeliveryScheduled(r.orderNumber());
        return toDto(delivery, names());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> listForCustomer(String customerId) {
        Map<String, String> names = names();
        return deliveryRepository.findByCustomerId(customerId).stream()
                .sorted(BY_TIME.reversed())
                .map(d -> toDto(d, names))
                .toList();
    }

    @Override
    public DeliveryDto cancelByCustomer(String customerId, Long deliveryId) {
        Delivery delivery = find(deliveryId);
        if (!delivery.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("You can only cancel your own deliveries.");
        }
        if (!customerCanCancel(delivery)) {
            throw new ValidationException("It is too late to cancel online (less than "
                    + settings.getCancelCutoffHours() + " hours before the slot). Please call the store.");
        }
        return cancel(delivery, "Cancelled by customer");
    }

    // =================================================================
    // Staff
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> listDeliveries(LocalDate date) {
        List<Delivery> deliveries = date == null
                ? deliveryRepository.findByDeliveryDateGreaterThanEqual(LocalDate.now(clock))
                : deliveryRepository.findByDeliveryDate(date);
        Map<String, String> names = names();
        return deliveries.stream().sorted(BY_TIME).map(d -> toDto(d, names)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverDto> listDrivers() {
        LocalDate today = LocalDate.now(clock);
        return driverRepository.findAllByOrderByActiveDescNameAsc().stream()
                .map(d -> DriverDto.from(d, deliveryRepository
                        .findByDriverIdAndDeliveryDateGreaterThanEqualAndStatusIn(d.getId(), today, ROUTED).size()))
                .toList();
    }

    @Override
    public DriverDto addDriver(DriverRequest r) {
        List<String> errors = new ArrayList<>();
        String name = r.name() == null ? "" : r.name().trim();
        String phone = r.phone() == null ? "" : r.phone().replaceAll("\\D", "");
        if (!name.matches("[A-Za-z][A-Za-z .'-]{1,59}")) {
            errors.add("Enter the driver's name (letters only).");
        }
        if (!phone.matches("\\d{7}")) {
            errors.add("Driver phone must be 7 digits.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String vehicle = r.vehicle() == null || r.vehicle().isBlank() ? null : r.vehicle().trim();
        return DriverDto.from(driverRepository.save(new Driver(name, phone, vehicle)), 0);
    }

    @Override
    public DriverDto setDriverActive(Long driverId, boolean active) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver " + driverId + " was not found."));
        driver.setActive(active);
        if (!active) {
            // Remove the driver from every upcoming delivery so staff can reassign them.
            List<Delivery> upcoming = deliveryRepository.findByDriverIdAndDeliveryDateGreaterThanEqualAndStatusIn(
                    driverId, LocalDate.now(clock), ROUTED);
            for (Delivery d : upcoming) {
                d.removeDriver(directEstimate(d));
            }
        }
        return DriverDto.from(driverRepository.save(driver), 0);
    }

    @Override
    @Transactional(readOnly = true)
    public ConflictCheckDto checkAssignment(Long deliveryId, Long driverId) {
        Delivery delivery = find(deliveryId);
        Driver driver = findDriver(driverId);
        List<Delivery> others = otherStops(driver, delivery);
        List<Delivery> withNew = new ArrayList<>(others);
        withNew.add(delivery);
        RoutePlanner.RoutePlan plan = plan(withNew, delivery);
        List<String> conflicts = conflictDetector.assignmentConflicts(delivery, driver, others,
                plan.fitsInSlot(), lastEta(plan));
        return new ConflictCheckDto(conflicts.isEmpty(), conflicts,
                toRouteDto(driver, delivery.getDeliveryDate(), delivery.getSlot(), plan, withNew));
    }

    @Override
    public DeliveryDto assignDriver(Long deliveryId, Long driverId) {
        ConflictCheckDto check = checkAssignment(deliveryId, driverId);
        if (!check.ok()) {
            throw new ConflictException(check.conflicts());
        }
        Delivery delivery = find(deliveryId);
        Driver driver = findDriver(driverId);
        delivery.assignDriver(driver);
        replan(driver, delivery.getDeliveryDate(), delivery.getSlot());
        return toDto(delivery, names());
    }

    @Override
    public DeliveryDto removeDriver(Long deliveryId) {
        Delivery delivery = find(deliveryId);
        Driver driver = delivery.getDriver();
        delivery.removeDriver(directEstimate(delivery));
        if (driver != null) {
            replan(driver, delivery.getDeliveryDate(), delivery.getSlot());
        }
        return toDto(delivery, names());
    }

    @Override
    public DeliveryDto confirm(Long deliveryId) {
        Delivery delivery = find(deliveryId);
        delivery.confirm();
        return toDto(delivery, names());
    }

    @Override
    public DeliveryDto cancelByStaff(Long deliveryId, String reason) {
        String why = reason == null || reason.isBlank() ? "Cancelled by store" : reason.trim();
        return cancel(find(deliveryId), why);
    }

    @Override
    public DeliveryDto markDelivered(Long deliveryId) {
        Delivery delivery = find(deliveryId);
        delivery.markDelivered();
        orderService.markDelivered(delivery.getOrderNumber());
        return toDto(delivery, names());
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDto getRoute(Long driverId, LocalDate date, DeliverySlot slot) {
        if (date == null || slot == null) {
            throw new ValidationException("Choose a date and slot.");
        }
        Driver driver = findDriver(driverId);
        List<Delivery> stops = deliveryRepository.findByDriverIdAndDeliveryDateAndSlotAndStatusIn(
                driverId, date, slot, ROUTED);
        RoutePlanner.RoutePlan plan = routePlanner.plan(toStops(stops), slot.startOn(date), slot.endOn(date));
        return toRouteDto(driver, date, slot, plan, stops);
    }

    /** If a customer deletes their account, their upcoming deliveries are cancelled. */
    @EventListener
    public void onAccountDeleted(AccountDeletedEvent event) {
        for (Delivery d : deliveryRepository.findByCustomerIdAndStatusIn(event.customerId(), ACTIVE)) {
            Driver driver = d.getDriver();
            d.cancel("Customer account deleted");
            if (driver != null) {
                replan(driver, d.getDeliveryDate(), d.getSlot());
            }
        }
    }

    // =================================================================
    // Helpers
    // =================================================================

    private DeliveryDto cancel(Delivery delivery, String reason) {
        Driver driver = delivery.getDriver();
        delivery.cancel(reason);
        if (driver != null) {
            replan(driver, delivery.getDeliveryDate(), delivery.getSlot());
        }
        orderService.markDeliveryCancelled(delivery.getOrderNumber());
        return toDto(delivery, names());
    }

    /** Route Scheduling: re-orders a driver's stops and updates every arrival time. */
    private void replan(Driver driver, LocalDate date, DeliverySlot slot) {
        List<Delivery> stops = deliveryRepository.findByDriverIdAndDeliveryDateAndSlotAndStatusIn(
                driver.getId(), date, slot, ROUTED);
        RoutePlanner.RoutePlan plan = routePlanner.plan(toStops(stops), slot.startOn(date), slot.endOn(date));
        Map<Long, Delivery> byId = stops.stream().collect(Collectors.toMap(Delivery::getId, Function.identity()));
        for (RoutePlanner.PlannedStop stop : plan.stops()) {
            byId.get(stop.deliveryId()).setRoutePosition(stop.sequence(), stop.eta());
        }
    }

    private RoutePlanner.RoutePlan plan(List<Delivery> deliveries, Delivery reference) {
        return routePlanner.plan(toStops(deliveries), reference.getSlotStart(), reference.getSlotEnd());
    }

    private List<Delivery> otherStops(Driver driver, Delivery delivery) {
        return deliveryRepository.findByDriverIdAndDeliveryDateAndSlotAndStatusIn(
                        driver.getId(), delivery.getDeliveryDate(), delivery.getSlot(), ROUTED).stream()
                .filter(d -> !d.getId().equals(delivery.getId()))
                .toList();
    }

    private static List<RoutePlanner.Stop> toStops(List<Delivery> deliveries) {
        return deliveries.stream()
                .map(d -> new RoutePlanner.Stop(d.getId(), d.getOrderNumber(), d.getArea().getLocation()))
                .toList();
    }

    private static LocalDateTime lastEta(RoutePlanner.RoutePlan plan) {
        return plan.stops().isEmpty() ? null : plan.stops().get(plan.stops().size() - 1).eta();
    }

    private RouteDto toRouteDto(Driver driver, LocalDate date, DeliverySlot slot, RoutePlanner.RoutePlan plan,
                                List<Delivery> deliveries) {
        Map<Long, Delivery> byId = deliveries.stream().collect(Collectors.toMap(Delivery::getId, Function.identity()));
        List<RouteDto.Stop> stops = plan.stops().stream().map(s -> {
            Delivery d = byId.get(s.deliveryId());
            return new RouteDto.Stop(s.sequence(), d.getId(), d.getOrderNumber(), d.getArea().getName(),
                    d.getAddress(), s.legKm(), s.legMinutes(), s.eta());
        }).toList();
        return new RouteDto(driver.getId(), driver.getName(), date, slot, slot.getLabel(), stops, plan.totalKm(),
                plan.totalMinutes(), plan.finishTime(), plan.fitsInSlot());
    }

    private LocalDateTime directEstimate(Delivery d) {
        return routePlanner.directEstimate(d.getArea().getLocation(), d.getSlotStart());
    }

    private boolean customerCanCancel(Delivery d) {
        return d.getStatus().isActive()
                && d.getSlotStart().isAfter(LocalDateTime.now(clock).plusHours(settings.getCancelCutoffHours()));
    }

    private Delivery find(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery " + id + " was not found."));
    }

    private Driver findDriver(Long id) {
        if (id == null) {
            throw new ValidationException("Choose a driver.");
        }
        return driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver " + id + " was not found."));
    }

    private Map<String, String> names() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(u -> String.valueOf(u.getId()), User::getFullName));
    }

    private DeliveryDto toDto(Delivery d, Map<String, String> names) {
        return DeliveryDto.from(d, names.getOrDefault(d.getCustomerId(), "Customer"), customerCanCancel(d));
    }
}
