package com.quickdeliver.service;

import com.quickdeliver.dto.AuthResponse;
import com.quickdeliver.dto.LoginRequest;
import com.quickdeliver.entity.*;
import com.quickdeliver.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RiderRepository riderRepository;

    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        Long entityId = user.getId();
        String displayName = user.getUsername();
        String zoneCode = "N/A";

        switch (user.getRole()) {
            case CUSTOMER:
                Optional<Customer> c = customerRepository.findByUser(user);
                if (c.isPresent()) {
                    entityId = c.get().getId();
                    displayName = c.get().getName();
                    zoneCode = c.get().getDeliveryZone().getZoneCode();
                }
                break;
            case RESTAURANT:
                Optional<Restaurant> r = restaurantRepository.findByUser(user);
                if (r.isPresent()) {
                    entityId = r.get().getId();
                    displayName = r.get().getName();
                    zoneCode = r.get().getDeliveryZone().getZoneCode();
                }
                break;
            case RIDER:
                Optional<Rider> rd = riderRepository.findByUser(user);
                if (rd.isPresent()) {
                    entityId = rd.get().getId();
                    displayName = rd.get().getName();
                    zoneCode = rd.get().getCurrentZone().getZoneCode();
                }
                break;
            case ADMIN:
                displayName = "Administrator";
                zoneCode = "ALL";
                break;
        }

        return new AuthResponse(user.getId(), user.getUsername(), user.getRole(), entityId, displayName, zoneCode);
    }
}
