package net.codejava.delivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import net.codejava.common.exception.ValidationException;

/** A booked delivery for one order (Module 9). */
@Entity
@Table(name = "delivery")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "area_id")
    private DeliveryArea area;

    @Column(nullable = false)
    private String address;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliverySlot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    /** Position in the driver's route for this slot (1 = first stop). */
    @Column(name = "route_sequence")
    private Integer routeSequence;

    /** Estimated Delivery Time. */
    @Column(name = "estimated_arrival")
    private LocalDateTime estimatedArrival;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Delivery() {
    }

    /** Schedule Delivery Order. */
    public Delivery(String orderNumber, String customerId, DeliveryArea area, String address, String contactPhone,
                    LocalDate deliveryDate, DeliverySlot slot, LocalDateTime estimatedArrival) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.area = area;
        this.address = address;
        this.contactPhone = contactPhone;
        this.deliveryDate = deliveryDate;
        this.slot = slot;
        this.estimatedArrival = estimatedArrival;
        this.status = DeliveryStatus.SCHEDULED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    /** Driver Assignment. */
    public void assignDriver(Driver driver) {
        requireActive();
        this.driver = driver;
        this.status = DeliveryStatus.ASSIGNED;
        touch();
    }

    /** Removed Appointed Delivery Driver: back to waiting for a driver. */
    public void removeDriver(LocalDateTime directEstimate) {
        requireActive();
        if (driver == null) {
            throw new ValidationException("This delivery has no driver to remove.");
        }
        this.driver = null;
        this.routeSequence = null;
        this.estimatedArrival = directEstimate;
        this.status = DeliveryStatus.SCHEDULED;
        touch();
    }

    /** Confirm Delivery Order Schedule: needs a driver first. */
    public void confirm() {
        if (status != DeliveryStatus.ASSIGNED) {
            throw new ValidationException(status == DeliveryStatus.CONFIRMED
                    ? "This delivery is already confirmed."
                    : "Assign a driver before confirming the schedule.");
        }
        this.status = DeliveryStatus.CONFIRMED;
        touch();
    }

    /** Cancel Schedule. */
    public void cancel(String reason) {
        requireActive();
        this.status = DeliveryStatus.CANCELLED;
        this.cancelReason = reason;
        this.routeSequence = null;
        touch();
    }

    public void markDelivered() {
        if (status != DeliveryStatus.CONFIRMED) {
            throw new ValidationException("Only confirmed deliveries can be marked as delivered.");
        }
        this.status = DeliveryStatus.DELIVERED;
        touch();
    }

    /** Route Scheduling result for this stop. */
    public void setRoutePosition(Integer sequence, LocalDateTime eta) {
        this.routeSequence = sequence;
        this.estimatedArrival = eta;
        touch();
    }

    public LocalDateTime getSlotStart() {
        return slot.startOn(deliveryDate);
    }

    public LocalDateTime getSlotEnd() {
        return slot.endOn(deliveryDate);
    }

    private void requireActive() {
        if (!status.isActive()) {
            throw new ValidationException("This delivery is " + status.name().toLowerCase() + " and cannot be changed.");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    /** Used by unit tests only. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public DeliveryArea getArea() {
        return area;
    }

    public String getAddress() {
        return address;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public DeliverySlot getSlot() {
        return slot;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Driver getDriver() {
        return driver;
    }

    public Integer getRouteSequence() {
        return routeSequence;
    }

    public LocalDateTime getEstimatedArrival() {
        return estimatedArrival;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
