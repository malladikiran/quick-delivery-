package com.quickdeliver.service;

import com.quickdeliver.dto.BfsResultDto;
import com.quickdeliver.entity.*;
import com.quickdeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ADSA & DMGT Core Implementation:
 * Breadth First Search (BFS) Rider Assignment Engine.
 * 
 * Mathematical Graph: G = (V, E)
 * V: Set of Delivery Zones (e.g. Z1, Z2, Z3, Z4, Z5, Z6)
 * E: Set of Undirected Edges representing physical adjacency between zones.
 * 
 * Time Complexity: O(|V| + |E|)
 * Space Complexity: O(|V|) for Queue and Visited Set.
 */
@Service
public class BfsRiderAssignmentService {

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private ZoneConnectionRepository connectionRepository;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private RiderLocationRepository riderLocationRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Builds the adjacency list representation of the unweighted graph G = (V, E).
     */
    public Map<Long, List<DeliveryZone>> buildAdjacencyList() {
        List<DeliveryZone> zones = zoneRepository.findAll();
        Map<Long, List<DeliveryZone>> adjList = new HashMap<>();

        for (DeliveryZone zone : zones) {
            adjList.put(zone.getId(), new ArrayList<>());
        }

        List<ZoneConnection> connections = connectionRepository.findAll();
        for (ZoneConnection conn : connections) {
            Long aId = conn.getZoneA().getId();
            Long bId = conn.getZoneB().getId();

            // Undirected graph: edge exists both ways
            if (adjList.containsKey(aId) && adjList.containsKey(bId)) {
                adjList.get(aId).add(conn.getZoneB());
                adjList.get(bId).add(conn.getZoneA());
            }
        }
        return adjList;
    }

    /**
     * Executes BFS starting from the restaurant/pickup zone to discover the nearest available rider.
     * 
     * @param orderId the order awaiting assignment
     * @return BfsResultDto containing assigned rider details, hop distance, and traversal path
     */
    @Transactional
    public BfsResultDto assignNearestRider(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Order must be in READY_FOR_PICKUP state for rider assignment. Current: " + order.getStatus());
        }

        DeliveryZone startZone = order.getRestaurant().getDeliveryZone();
        Map<Long, List<DeliveryZone>> graph = buildAdjacencyList();

        // BFS Data Structures
        Queue<DeliveryZone> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        Map<Long, Integer> hopDistanceMap = new HashMap<>();
        List<String> traversalPath = new ArrayList<>();

        // Initialize BFS from Start Zone
        queue.add(startZone);
        visited.add(startZone.getId());
        hopDistanceMap.put(startZone.getId(), 0);

        Rider selectedRider = null;
        DeliveryZone foundInZone = null;

        while (!queue.isEmpty()) {
            DeliveryZone currentZone = queue.poll();
            int currentHops = hopDistanceMap.get(currentZone.getId());
            traversalPath.add(currentZone.getZoneCode() + " (hop " + currentHops + ")");

            // Check if any active and available rider is in current zone
            List<Rider> availableRidersInZone = riderRepository.findByCurrentZoneAndIsAvailableTrueAndIsActiveTrue(currentZone);

            if (!availableRidersInZone.isEmpty()) {
                // Nearest rider discovered!
                selectedRider = availableRidersInZone.get(0);
                foundInZone = currentZone;
                break;
            }

            // Explore adjacent neighboring zones level by level
            List<DeliveryZone> neighbors = graph.getOrDefault(currentZone.getId(), Collections.emptyList());
            for (DeliveryZone neighbor : neighbors) {
                if (!visited.contains(neighbor.getId())) {
                    visited.add(neighbor.getId());
                    hopDistanceMap.put(neighbor.getId(), currentHops + 1);
                    queue.add(neighbor);
                }
            }
        }

        if (selectedRider != null) {
            // Update rider status to busy
            selectedRider.setIsAvailable(false);
            riderRepository.save(selectedRider);

            // Record rider location update
            riderLocationRepository.save(new RiderLocation(selectedRider, foundInZone));

            // Assign rider to order and advance status
            order.setRider(selectedRider);
            order.setStatus(OrderStatus.RIDER_ASSIGNED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            return BfsResultDto.success(
                    order.getId(),
                    selectedRider.getId(),
                    selectedRider.getName(),
                    selectedRider.getPhone(),
                    selectedRider.getVehicleType(),
                    startZone.getZoneCode(),
                    foundInZone.getZoneCode(),
                    hopDistanceMap.get(foundInZone.getId()),
                    traversalPath
            );
        } else {
            return BfsResultDto.failure(
                    order.getId(),
                    startZone.getZoneCode(),
                    traversalPath,
                    "No available riders found in any connected delivery zones."
            );
        }
    }
}
