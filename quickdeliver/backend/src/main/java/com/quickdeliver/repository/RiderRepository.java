package com.quickdeliver.repository;

import com.quickdeliver.entity.DeliveryZone;
import com.quickdeliver.entity.Rider;
import com.quickdeliver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByUser(User user);
    Optional<Rider> findByUserId(Long userId);
    List<Rider> findByCurrentZoneAndIsAvailableTrueAndIsActiveTrue(DeliveryZone zone);
    List<Rider> findByCurrentZoneIdAndIsAvailableTrueAndIsActiveTrue(Long zoneId);
    List<Rider> findByIsAvailableTrueAndIsActiveTrue();
}
