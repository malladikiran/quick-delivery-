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
@RequestMapping("/api/restaurant")
@CrossOrigin(origins = "*")
public class RestaurantController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @GetMapping("/{id}/orders")
    public List<Order> getRestaurantOrders(@PathVariable Long id) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(id);
    }

    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            Order order = orderService.confirmOrder(id);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/preparing")
    public ResponseEntity<?> preparingOrder(@PathVariable Long id) {
        try {
            Order order = orderService.markPreparing(id);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/ready")
    public ResponseEntity<?> readyForPickup(@PathVariable Long id) {
        try {
            Order order = orderService.markReadyForPickup(id);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
