package com.quickdeliver.controller;

import com.quickdeliver.entity.EtaPrediction;
import com.quickdeliver.entity.Order;
import com.quickdeliver.repository.EtaPredictionRepository;
import com.quickdeliver.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "*")
public class TrackingController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private EtaPredictionRepository etaPredictionRepository;

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderTracking(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrder(orderId);
            Optional<EtaPrediction> eta = etaPredictionRepository.findByOrderId(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("order", order);
            response.put("etaPrediction", eta.orElse(null));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
