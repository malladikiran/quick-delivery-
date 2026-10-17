package com.quickdeliver.controller;

import com.quickdeliver.dto.BfsResultDto;
import com.quickdeliver.dto.ZoneGraphDto;
import com.quickdeliver.entity.*;
import com.quickdeliver.repository.*;
import com.quickdeliver.service.BfsRiderAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dispatch")
@CrossOrigin(origins = "*")
public class AdminDispatchController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private ZoneConnectionRepository connectionRepository;

    @Autowired
    private BfsRiderAssignmentService bfsService;

    @GetMapping("/ready-orders")
    public List<Order> getReadyOrders() {
        return orderRepository.findByStatus(OrderStatus.READY_FOR_PICKUP);
    }

    @GetMapping("/all-orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/riders")
    public List<Rider> getRiders() {
        return riderRepository.findAll();
    }

    @GetMapping("/zones-graph")
    public ZoneGraphDto getZonesGraph() {
        List<DeliveryZone> zones = zoneRepository.findAll();
        List<Rider> availableRiders = riderRepository.findByIsAvailableTrueAndIsActiveTrue();

        Map<Long, List<String>> ridersByZone = new HashMap<>();
        for (Rider rider : availableRiders) {
            Long zId = rider.getCurrentZone().getId();
            ridersByZone.computeIfAbsent(zId, k -> new ArrayList<>()).add(rider.getName() + " (" + rider.getVehicleType() + ")");
        }

        List<ZoneGraphDto.ZoneNode> nodes = zones.stream().map(z -> {
            List<String> riderNames = ridersByZone.getOrDefault(z.getId(), Collections.emptyList());
            return new ZoneGraphDto.ZoneNode(z.getId(), z.getZoneCode(), z.getName(), riderNames.size(), riderNames);
        }).collect(Collectors.toList());

        List<ZoneConnection> connections = connectionRepository.findAll();
        List<ZoneGraphDto.ZoneEdge> edges = connections.stream().map(conn ->
                new ZoneGraphDto.ZoneEdge(conn.getZoneA().getZoneCode(), conn.getZoneB().getZoneCode())
        ).collect(Collectors.toList());

        return new ZoneGraphDto(nodes, edges);
    }

    @PostMapping("/{orderId}/run-bfs")
    public ResponseEntity<?> runBfsAssignment(@PathVariable Long orderId) {
        try {
            BfsResultDto result = bfsService.assignNearestRider(orderId);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
