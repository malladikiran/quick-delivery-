package com.quickdeliver.controller;

import com.quickdeliver.dto.OrderRequest;
import com.quickdeliver.entity.*;
import com.quickdeliver.repository.*;
import com.quickdeliver.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @GetMapping("/restaurants")
    public List<Restaurant> getRestaurants() {
        return restaurantRepository.findAll();
    }

    @GetMapping("/restaurants/{id}/menu")
    public List<FoodItem> getMenu(@PathVariable Long id) {
        return foodItemRepository.findByRestaurantIdAndIsAvailableTrue(id);
    }

    @GetMapping("/zones")
    public List<DeliveryZone> getZones() {
        return zoneRepository.findAll();
    }

    @PostMapping("/orders")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            Order order = orderService.placeOrder(request);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/orders")
    public List<Order> getCustomerOrders(@RequestParam Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
