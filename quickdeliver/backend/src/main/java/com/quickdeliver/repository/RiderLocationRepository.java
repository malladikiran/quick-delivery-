package com.quickdeliver.repository;

import com.quickdeliver.entity.RiderLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RiderLocationRepository extends JpaRepository<RiderLocation, Long> {
    List<RiderLocation> findByRiderIdOrderByUpdatedAtDesc(Long riderId);
}
