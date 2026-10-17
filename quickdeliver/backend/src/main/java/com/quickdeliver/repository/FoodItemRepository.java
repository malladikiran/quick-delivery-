package com.quickdeliver.repository;

import com.quickdeliver.entity.FoodItem;
import com.quickdeliver.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByRestaurant(Restaurant restaurant);
    List<FoodItem> findByRestaurantIdAndIsAvailableTrue(Long restaurantId);
}
