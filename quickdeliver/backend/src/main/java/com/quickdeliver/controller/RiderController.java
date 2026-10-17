package com.quickdeliver.controller;

import com.quickdeliver.entity.Order;
import com.quickdeliver.repository.OrderRepository;
import com.quickdeliver.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rider")
@CrossOrigin(origins = "*")
public class RiderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @GetMapping("/{id}/assigned-orders")
    public List<Order> getAssignedOrders(@PathVariable Long id) {
        return orderRepository.findByRiderIdOrderByCreatedAtDesc(id);
    }

    @PostMapping("/orders/{id}/pickup")
    public ResponseEntity<?> pickupOrder(@PathVariable Long id, @RequestParam Long riderId) {
        try {
            Order order = orderService.markPickedUp(id, riderId);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/out-for-delivery")
    public ResponseEntity<?> outForDelivery(@PathVariable Long id, @RequestParam Long riderId) {
        try {
            Order order = orderService.markOutForDelivery(id, riderId);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable Long id, @RequestParam Long riderId) {
        try {
            Order order = orderService.markDelivered(id, riderId);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
