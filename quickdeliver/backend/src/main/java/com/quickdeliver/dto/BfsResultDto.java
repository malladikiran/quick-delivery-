package com.quickdeliver.dto;

import java.util.List;

public class BfsResultDto {
    private boolean success;
    private Long orderId;
    private Long riderId;
    private String riderName;
    private String riderPhone;
    private String vehicleType;
    private String startZone;
    private String foundZone;
    private int hopDistance;
    private List<String> traversalPath;
    private String message;

    public BfsResultDto() {}

    public static BfsResultDto success(Long orderId, Long riderId, String riderName, String riderPhone,
                                      String vehicleType, String startZone, String foundZone,
                                      int hopDistance, List<String> traversalPath) {
        BfsResultDto dto = new BfsResultDto();
        dto.success = true;
        dto.orderId = orderId;
        dto.riderId = riderId;
        dto.riderName = riderName;
        dto.riderPhone = riderPhone;
        dto.vehicleType = vehicleType;
        dto.startZone = startZone;
        dto.foundZone = foundZone;
        dto.hopDistance = hopDistance;
        dto.traversalPath = traversalPath;
        dto.message = "Successfully assigned nearest rider via BFS search (Hops: " + hopDistance + ")";
        return dto;
    }

    public static BfsResultDto failure(Long orderId, String startZone, List<String> traversalPath, String reason) {
        BfsResultDto dto = new BfsResultDto();
        dto.success = false;
        dto.orderId = orderId;
        dto.startZone = startZone;
        dto.traversalPath = traversalPath;
        dto.message = reason;
        return dto;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }

    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }

    public String getRiderPhone() { return riderPhone; }
    public void setRiderPhone(String riderPhone) { this.riderPhone = riderPhone; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getStartZone() { return startZone; }
    public void setStartZone(String startZone) { this.startZone = startZone; }

    public String getFoundZone() { return foundZone; }
    public void setFoundZone(String foundZone) { this.foundZone = foundZone; }

    public int getHopDistance() { return hopDistance; }
    public void setHopDistance(int hopDistance) { this.hopDistance = hopDistance; }

    public List<String> getTraversalPath() { return traversalPath; }
    public void setTraversalPath(List<String> traversalPath) { this.traversalPath = traversalPath; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
