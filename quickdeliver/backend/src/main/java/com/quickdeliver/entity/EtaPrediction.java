package com.quickdeliver.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eta_predictions")
public class EtaPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "distance_km", nullable = false, precision = 5, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "hour_of_day", nullable = false)
    private Integer hourOfDay;

    @Column(name = "predicted_mins", nullable = false)
    private Integer predictedMins;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public EtaPrediction() {}

    public EtaPrediction(Order order, BigDecimal distanceKm, Integer hourOfDay, Integer predictedMins) {
        this.order = order;
        this.distanceKm = distanceKm;
        this.hourOfDay = hourOfDay;
        this.predictedMins = predictedMins;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public Integer getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }

    public Integer getPredictedMins() { return predictedMins; }
    public void setPredictedMins(Integer predictedMins) { this.predictedMins = predictedMins; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
