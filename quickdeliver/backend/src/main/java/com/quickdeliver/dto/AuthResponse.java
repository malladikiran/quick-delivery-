package com.quickdeliver.dto;

import com.quickdeliver.entity.Role;

public class AuthResponse {
    private Long userId;
    private String username;
    private Role role;
    private Long entityId; // customerId, restaurantId, riderId, or admin id
    private String displayName;
    private String zoneCode;

    public AuthResponse() {}

    public AuthResponse(Long userId, String username, Role role, Long entityId, String displayName, String zoneCode) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.entityId = entityId;
        this.displayName = displayName;
        this.zoneCode = zoneCode;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
}
