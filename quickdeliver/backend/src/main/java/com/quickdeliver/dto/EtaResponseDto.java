package com.quickdeliver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class EtaResponseDto {
    private String status;

    @JsonProperty("distance_km")
    private BigDecimal distanceKm;

    @JsonProperty("hour_of_day")
    private Integer hourOfDay;

    @JsonProperty("eta_minutes")
    private Integer etaMinutes;

    private String algorithm;

    public EtaResponseDto() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public Integer getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }

    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}
