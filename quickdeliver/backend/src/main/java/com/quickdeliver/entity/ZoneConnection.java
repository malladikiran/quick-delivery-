package com.quickdeliver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "zone_connections")
public class ZoneConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_a_id", nullable = false)
    private DeliveryZone zoneA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_b_id", nullable = false)
    private DeliveryZone zoneB;

    public ZoneConnection() {}

    public ZoneConnection(DeliveryZone zoneA, DeliveryZone zoneB) {
        this.zoneA = zoneA;
        this.zoneB = zoneB;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DeliveryZone getZoneA() { return zoneA; }
    public void setZoneA(DeliveryZone zoneA) { this.zoneA = zoneA; }

    public DeliveryZone getZoneB() { return zoneB; }
    public void setZoneB(DeliveryZone zoneB) { this.zoneB = zoneB; }
}
