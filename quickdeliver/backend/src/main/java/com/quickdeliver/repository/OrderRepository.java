package com.quickdeliver.repository;

import com.quickdeliver.entity.Order;
import com.quickdeliver.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    List<Order> findByRiderIdOrderByCreatedAtDesc(Long riderId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();
}
