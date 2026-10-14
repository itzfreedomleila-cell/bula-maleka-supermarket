package net.codejava.delivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** A delivery driver. */
@Entity
@Table(name = "driver")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String vehicle;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Driver() {
    }

    public Driver(String name, String phone, String vehicle) {
        this.name = name;
        this.phone = phone;
        this.vehicle = vehicle;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    /** Used by unit tests only. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getVehicle() {
        return vehicle;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
