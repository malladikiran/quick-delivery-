package com.quickdeliver.dto;

import java.util.List;

public class OrderRequest {
    private Long customerId;
    private Long restaurantId;
    private Long deliveryZoneId;
    private List<OrderItemRequest> items;

    public OrderRequest() {}

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public Long getDeliveryZoneId() { return deliveryZoneId; }
    public void setDeliveryZoneId(Long deliveryZoneId) { this.deliveryZoneId = deliveryZoneId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
