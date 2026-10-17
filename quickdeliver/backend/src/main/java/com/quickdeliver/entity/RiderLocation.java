package com.quickdeliver.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rider_locations")
public class RiderLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rider_id", nullable = false)
    private Rider rider;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id", nullable = false)
    private DeliveryZone zone;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RiderLocation() {}

    public RiderLocation(Rider rider, DeliveryZone zone) {
        this.rider = rider;
        this.zone = zone;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Rider getRider() { return rider; }
    public void setRider(Rider rider) { this.rider = rider; }

    public DeliveryZone getZone() { return zone; }
    public void setZone(DeliveryZone zone) { this.zone = zone; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
