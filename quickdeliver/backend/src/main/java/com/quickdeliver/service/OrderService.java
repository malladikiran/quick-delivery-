package com.quickdeliver.service;

import com.quickdeliver.dto.OrderItemRequest;
import com.quickdeliver.dto.OrderRequest;
import com.quickdeliver.entity.*;
import com.quickdeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private RiderLocationRepository riderLocationRepository;

    @Autowired
    private PythonEtaClientService etaClientService;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.getCustomerId()));
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + request.getRestaurantId()));
        DeliveryZone deliveryZone = deliveryZoneRepository.findById(request.getDeliveryZoneId())
                .orElseThrow(() -> new IllegalArgumentException("Delivery zone not found with ID: " + request.getDeliveryZoneId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one food item.");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryZone(deliveryZone);
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Compute simulated distance between restaurant zone and delivery zone
        long zoneDiff = Math.abs(restaurant.getDeliveryZone().getId() - deliveryZone.getId());
        double calculatedDist = 2.0 + (zoneDiff * 1.8);
        order.setDistanceKm(BigDecimal.valueOf(calculatedDist));

        for (OrderItemRequest itemReq : request.getItems()) {
            FoodItem foodItem = foodItemRepository.findById(itemReq.getFoodItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Food item not found: " + itemReq.getFoodItemId()));
            BigDecimal lineTotal = foodItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = new OrderItem(foodItem, itemReq.getQuantity(), foodItem.getPrice());
            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = getOrder(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markPreparing(Long orderId) {
        Order order = getOrder(orderId);
        order.setStatus(OrderStatus.PREPARING);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markReadyForPickup(Long orderId) {
        Order order = getOrder(orderId);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        Order savedOrder = orderRepository.save(order);

        // Compute & persist predicted ETA via Python Regression Service
        etaClientService.calculateAndSaveEta(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order markPickedUp(Long orderId, Long riderId) {
        Order order = getOrder(orderId);
        verifyRider(order, riderId);
        order.setStatus(OrderStatus.PICKED_UP);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markOutForDelivery(Long orderId, Long riderId) {
        Order order = getOrder(orderId);
        verifyRider(order, riderId);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markDelivered(Long orderId, Long riderId) {
        Order order = getOrder(orderId);
        verifyRider(order, riderId);
        order.setStatus(OrderStatus.DELIVERED);
        Order savedOrder = orderRepository.save(order);

        // Free rider and update rider's current zone to the delivered location
        Rider rider = order.getRider();
        if (rider != null) {
            rider.setIsAvailable(true);
            rider.setCurrentZone(order.getDeliveryZone());
            riderRepository.save(rider);
            riderLocationRepository.save(new RiderLocation(rider, order.getDeliveryZone()));
        }

        return savedOrder;
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }

    private void verifyRider(Order order, Long riderId) {
        if (order.getRider() == null || !order.getRider().getId().equals(riderId)) {
            throw new IllegalStateException("Rider ID " + riderId + " is not authorized for this order.");
        }
    }
}
