package com.quickdeliver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "riders")
public class Rider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_zone_id", nullable = false)
    private DeliveryZone currentZone;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public Rider() {}

    public Rider(User user, String name, String phone, String vehicleType, DeliveryZone currentZone) {
        this.user = user;
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.currentZone = currentZone;
        this.isAvailable = true;
        this.isActive = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public DeliveryZone getCurrentZone() { return currentZone; }
    public void setCurrentZone(DeliveryZone currentZone) { this.currentZone = currentZone; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
