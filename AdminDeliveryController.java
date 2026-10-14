package net.codejava.delivery.controller;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import net.codejava.account.security.CurrentUser;
import net.codejava.delivery.dto.AssignDriverRequest;
import net.codejava.delivery.dto.CancelRequest;
import net.codejava.delivery.dto.ConflictCheckDto;
import net.codejava.delivery.dto.DeliveryDto;
import net.codejava.delivery.dto.DriverDto;
import net.codejava.delivery.dto.DriverRequest;
import net.codejava.delivery.dto.RouteDto;
import net.codejava.delivery.model.DeliverySlot;
import net.codejava.delivery.service.DeliveryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for Module 9 - Delivery (staff side). */
@RestController
@RequestMapping("/api/admin/delivery")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;
    private final CurrentUser currentUser;

    public AdminDeliveryController(DeliveryService deliveryService, CurrentUser currentUser) {
        this.deliveryService = deliveryService;
        this.currentUser = currentUser;
    }

    @GetMapping("/deliveries")
    public List<DeliveryDto> deliveries(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.listDeliveries(date);
    }

    @GetMapping("/drivers")
    public List<DriverDto> drivers(HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.listDrivers();
    }

    @PostMapping("/drivers")
    @ResponseStatus(HttpStatus.CREATED)
    public DriverDto addDriver(@RequestBody DriverRequest request, HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.addDriver(request);
    }

    @PostMapping("/drivers/{id}/active")
    public DriverDto setActive(@PathVariable("id") Long id, @RequestParam("value") boolean active,
                               HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.setDriverActive(id, active);
    }

    /** Delivery Conflict Detection (preview). */
    @GetMapping("/deliveries/{id}/conflicts")
    public ConflictCheckDto conflicts(@PathVariable("id") Long id, @RequestParam("driverId") Long driverId,
                                      HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.checkAssignment(id, driverId);
    }

    /** Driver Assignment. */
    @PostMapping("/deliveries/{id}/assign")
    public DeliveryDto assign(@PathVariable("id") Long id, @RequestBody AssignDriverRequest request,
                              HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.assignDriver(id, request.driverId());
    }

    /** Removed Appointed Delivery Driver. */
    @PostMapping("/deliveries/{id}/remove-driver")
    public DeliveryDto removeDriver(@PathVariable("id") Long id, HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.removeDriver(id);
    }

    /** Confirm Delivery Order Schedule. */
    @PostMapping("/deliveries/{id}/confirm")
    public DeliveryDto confirm(@PathVariable("id") Long id, HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.confirm(id);
    }

    /** Cancel Schedule. */
    @PostMapping("/deliveries/{id}/cancel")
    public DeliveryDto cancel(@PathVariable("id") Long id, @RequestBody(required = false) CancelRequest request,
                              HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.cancelByStaff(id, request == null ? null : request.reason());
    }

    @PostMapping("/deliveries/{id}/delivered")
    public DeliveryDto delivered(@PathVariable("id") Long id, HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.markDelivered(id);
    }

    /** Route Scheduling and Estimated Delivery Times for one driver and slot. */
    @GetMapping("/routes")
    public RouteDto route(@RequestParam("driverId") Long driverId,
                          @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam("slot") DeliverySlot slot, HttpSession session) {
        currentUser.requireAdmin(session);
        return deliveryService.getRoute(driverId, date, slot);
    }
}
