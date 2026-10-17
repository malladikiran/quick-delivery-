package com.quickdeliver.config;

import com.quickdeliver.entity.*;
import com.quickdeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private DeliveryZoneRepository zoneRepository;
    @Autowired private ZoneConnectionRepository connectionRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private FoodItemRepository foodItemRepository;
    @Autowired private RiderRepository riderRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EtaPredictionRepository etaPredictionRepository;

    @Override
    public void run(String... args) {
        if (zoneRepository.count() > 0) return;

        System.out.println(">>> Initializing QuickDeliver seed data...");

        // 1. Delivery Zones (V)
        DeliveryZone z1 = zoneRepository.save(new DeliveryZone("Z1", "Downtown Central", "Commercial hub and restaurant alley"));
        DeliveryZone z2 = zoneRepository.save(new DeliveryZone("Z2", "Tech Corridor", "IT parks, high-rises and business towers"));
        DeliveryZone z3 = zoneRepository.save(new DeliveryZone("Z3", "University Campus", "Student colleges, hostels and food courts"));
        DeliveryZone z4 = zoneRepository.save(new DeliveryZone("Z4", "South Hills", "High-density residential apartments"));
        DeliveryZone z5 = zoneRepository.save(new DeliveryZone("Z5", "Old Market Square", "Bustling bazaars and metro interchange"));
        DeliveryZone z6 = zoneRepository.save(new DeliveryZone("Z6", "Eastern Suburbs", "Quiet peripheral residential colony"));

        // 2. Zone Connections (E) - Undirected graph
        // Z1 -- Z2, Z1 -- Z3, Z2 -- Z4, Z3 -- Z4, Z3 -- Z5, Z4 -- Z5, Z5 -- Z6
        connectionRepository.save(new ZoneConnection(z1, z2));
        connectionRepository.save(new ZoneConnection(z1, z3));
        connectionRepository.save(new ZoneConnection(z2, z4));
        connectionRepository.save(new ZoneConnection(z3, z4));
        connectionRepository.save(new ZoneConnection(z3, z5));
        connectionRepository.save(new ZoneConnection(z4, z5));
        connectionRepository.save(new ZoneConnection(z5, z6));

        // 3. Users & Entities
        // Admin
        userRepository.save(new User("admin", "admin123", Role.ADMIN));

        // Customers
        User uCust1 = userRepository.save(new User("rahul", "pass123", Role.CUSTOMER));
        Customer cust1 = customerRepository.save(new Customer(uCust1, "Rahul Sharma", "9876543210", "Flat 402, Sunshine Heights", z4));

        User uCust2 = userRepository.save(new User("priya", "pass123", Role.CUSTOMER));
        Customer cust2 = customerRepository.save(new Customer(uCust2, "Priya Patel", "9876543211", "Hostel Block B, Univ Campus", z3));

        // Restaurants
        User uRest1 = userRepository.save(new User("spicekitchen", "pass123", Role.RESTAURANT));
        Restaurant rest1 = restaurantRepository.save(new Restaurant(uRest1, "The Spice Kitchen", "9876543220", "Shop 12, Food Street, Z1", z1));

        User uRest2 = userRepository.save(new User("urbanpizza", "pass123", Role.RESTAURANT));
        Restaurant rest2 = restaurantRepository.save(new Restaurant(uRest2, "Urban Woodfire Pizza", "9876543221", "Tower 4 Ground, Tech Park, Z2", z2));

        // Food Items for Spice Kitchen
        FoodItem fi1 = foodItemRepository.save(new FoodItem(rest1, "Paneer Butter Masala", "Rich creamy gravy with cottage cheese cubes", BigDecimal.valueOf(280.00), true));
        FoodItem fi2 = foodItemRepository.save(new FoodItem(rest1, "Butter Naan (2 pcs)", "Crispy clay oven flatbread with farm butter", BigDecimal.valueOf(90.00), true));
        FoodItem fi3 = foodItemRepository.save(new FoodItem(rest1, "Hyderabadi Dum Biryani", "Fragrant basmati rice layered with spiced veggies", BigDecimal.valueOf(320.00), true));
        FoodItem fi4 = foodItemRepository.save(new FoodItem(rest1, "Gulab Jamun (2 pcs)", "Warm milk dumplings in saffron syrup", BigDecimal.valueOf(80.00), true));

        // Food Items for Urban Pizza
        foodItemRepository.save(new FoodItem(rest2, "Margherita Basil Pizza", "San Marzano tomatoes, fresh mozzarella, sweet basil", BigDecimal.valueOf(350.00), true));
        foodItemRepository.save(new FoodItem(rest2, "Spicy Peri-Peri Chicken Pizza", "Smoked chicken, peri-peri drizzle, bell peppers", BigDecimal.valueOf(450.00), true));
        foodItemRepository.save(new FoodItem(rest2, "Cheesy Garlic Sticks", "Pull-apart garlic loaf stuffed with cheddar", BigDecimal.valueOf(160.00), true));

        // Riders in strategic zones for BFS demonstration:
        // Notice: Restaurant 1 is in Z1.
        // Neighbors of Z1 are Z2 and Z3.
        // Let's place Rider 1 in Z2 (hop 1)
        User uRider1 = userRepository.save(new User("rider_karan", "pass123", Role.RIDER));
        riderRepository.save(new Rider(uRider1, "Karan Verma", "9876543230", "Yamaha FZ (Bike)", z2));

        // Rider 2 in Z4 (hop 2 from Z1)
        User uRider2 = userRepository.save(new User("rider_anita", "pass123", Role.RIDER));
        riderRepository.save(new Rider(uRider2, "Anita Roy", "9876543231", "Honda Activa (Scooter)", z4));

        // Rider 3 in Z5 (hop 2 from Z1 via Z3)
        User uRider3 = userRepository.save(new User("rider_vikram", "pass123", Role.RIDER));
        riderRepository.save(new Rider(uRider3, "Vikram Singh", "9876543232", "Bajaj Pulsar (Bike)", z5));

        // Sample initial ready order so the user/examiner can run BFS immediately!
        Order readyOrder = new Order(cust1, rest1, z4, BigDecimal.valueOf(370.00), BigDecimal.valueOf(5.6));
        readyOrder.setStatus(OrderStatus.READY_FOR_PICKUP);
        readyOrder.addItem(new OrderItem(fi1, 1, BigDecimal.valueOf(280.00)));
        readyOrder.addItem(new OrderItem(fi2, 1, BigDecimal.valueOf(90.00)));
        Order savedOrder = orderRepository.save(readyOrder);

        // Pre-compute ETA for sample order (Regression estimate)
        etaPredictionRepository.save(new EtaPrediction(savedOrder, BigDecimal.valueOf(5.6), LocalDateTime.now().getHour(), 28));

        System.out.println(">>> QuickDeliver seed data created successfully!");
    }
}
