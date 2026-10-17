package com.quickdeliver.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

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

    @Column(nullable = false, length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id", nullable = false)
    private DeliveryZone deliveryZone;

    public Customer() {}

    public Customer(User user, String name, String phone, String address, DeliveryZone deliveryZone) {
        this.user = user;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.deliveryZone = deliveryZone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public DeliveryZone getDeliveryZone() { return deliveryZone; }
    public void setDeliveryZone(DeliveryZone deliveryZone) { this.deliveryZone = deliveryZone; }
}
