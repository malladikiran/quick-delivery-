package com.quickdeliver.repository;

import com.quickdeliver.entity.EtaPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EtaPredictionRepository extends JpaRepository<EtaPrediction, Long> {
    Optional<EtaPrediction> findByOrderId(Long orderId);
}
