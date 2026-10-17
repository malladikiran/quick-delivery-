package com.quickdeliver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_zones")
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_code", nullable = false, unique = true, length = 10)
    private String zoneCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    public DeliveryZone() {}

    public DeliveryZone(String zoneCode, String name, String description) {
        this.zoneCode = zoneCode;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
