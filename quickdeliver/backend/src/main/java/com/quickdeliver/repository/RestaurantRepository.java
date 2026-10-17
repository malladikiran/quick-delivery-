package com.quickdeliver.repository;

import com.quickdeliver.entity.Restaurant;
import com.quickdeliver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByUser(User user);
    Optional<Restaurant> findByUserId(Long userId);
}
