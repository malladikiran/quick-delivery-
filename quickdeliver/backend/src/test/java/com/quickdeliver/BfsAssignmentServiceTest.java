package com.quickdeliver;

import com.quickdeliver.dto.BfsResultDto;
import com.quickdeliver.entity.*;
import com.quickdeliver.repository.*;
import com.quickdeliver.service.BfsRiderAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BfsAssignmentServiceTest {

    @Autowired
    private BfsRiderAssignmentService bfsService;

    @Autowired
    private DeliveryZoneRepository zoneRepository;

    @Autowired
    private ZoneConnectionRepository connectionRepository;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("ADSA BFS Test: Discovers nearest rider in adjacent hop 1 zone (Z2)")
    public void testBfsDiscoversNearestRiderHop1() {
        // Restaurant is in Z1
        Restaurant rest = restaurantRepository.findAll().get(0);
        Customer cust = customerRepository.findAll().get(0);

        Order order = new Order(cust, rest, cust.getDeliveryZone(), BigDecimal.valueOf(250.0), BigDecimal.valueOf(4.0));
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order = orderRepository.save(order);

        BfsResultDto result = bfsService.assignNearestRider(order.getId());

        assertTrue(result.isSuccess(), "BFS should successfully locate an available rider");
        assertEquals("Z1", result.getStartZone(), "BFS should start from Restaurant Zone Z1");
        assertEquals("Z2", result.getFoundZone(), "Nearest rider should be found in Z2");
        assertEquals(1, result.getHopDistance(), "Hop distance to Z2 must be exactly 1");
        assertNotNull(result.getRiderName());
        System.out.println("ADSA Test Traversal: " + result.getTraversalPath());
    }

    @Test
    @DisplayName("ADSA BFS Test: When no riders are available, BFS terminates cleanly")
    public void testBfsNoRidersAvailable() {
        // Mark all riders unavailable
        List<Rider> allRiders = riderRepository.findAll();
        for (Rider r : allRiders) {
            r.setIsAvailable(false);
            riderRepository.save(r);
        }

        Restaurant rest = restaurantRepository.findAll().get(0);
        Customer cust = customerRepository.findAll().get(0);

        Order order = new Order(cust, rest, cust.getDeliveryZone(), BigDecimal.valueOf(300.0), BigDecimal.valueOf(5.0));
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order = orderRepository.save(order);

        BfsResultDto result = bfsService.assignNearestRider(order.getId());

        assertFalse(result.isSuccess(), "BFS should cleanly handle exhaustion when zero riders are available");
        assertTrue(result.getMessage().contains("No available riders found"));
    }
}
