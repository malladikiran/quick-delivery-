package com.quickdeliver.repository;

import com.quickdeliver.entity.DeliveryZone;
import com.quickdeliver.entity.ZoneConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ZoneConnectionRepository extends JpaRepository<ZoneConnection, Long> {

    @Query("SELECT zc FROM ZoneConnection zc WHERE zc.zoneA = :zone OR zc.zoneB = :zone")
    List<ZoneConnection> findAllConnectionsForZone(@Param("zone") DeliveryZone zone);
}
