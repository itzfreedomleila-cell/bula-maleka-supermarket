package net.codejava.delivery.controller;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import net.codejava.account.security.CurrentUser;
import net.codejava.delivery.dto.AreaDto;
import net.codejava.delivery.dto.DeliveryDto;
import net.codejava.delivery.dto.ScheduleDeliveryRequest;
import net.codejava.delivery.dto.SlotAvailabilityDto;
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

/** REST API for Module 9 - Delivery (customer side). */
@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CurrentUser currentUser;

    public DeliveryController(DeliveryService deliveryService, CurrentUser currentUser) {
        this.deliveryService = deliveryService;
        this.currentUser = currentUser;
    }

    @GetMapping("/areas")
    public List<AreaDto> areas() {
        return deliveryService.listAreas();
    }

    @GetMapping("/slots")
    public List<SlotAvailabilityDto> slots(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return deliveryService.slotAvailability(date);
    }

    /** Schedule Delivery Order. */
    @PostMapping("/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryDto schedule(@RequestBody ScheduleDeliveryRequest request, HttpSession session) {
        return deliveryService.schedule(currentUser.customerId(session), request);
    }

    @GetMapping("/mine")
    public List<DeliveryDto> mine(HttpSession session) {
        return deliveryService.listForCustomer(currentUser.customerId(session));
    }

    /** Cancel Schedule. */
    @PostMapping("/{id}/cancel")
    public DeliveryDto cancel(@PathVariable("id") Long id, HttpSession session) {
        return deliveryService.cancelByCustomer(currentUser.customerId(session), id);
    }
}
