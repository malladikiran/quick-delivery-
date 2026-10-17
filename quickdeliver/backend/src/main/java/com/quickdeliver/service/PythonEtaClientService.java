package com.quickdeliver.service;

import com.quickdeliver.dto.EtaResponseDto;
import com.quickdeliver.entity.EtaPrediction;
import com.quickdeliver.entity.Order;
import com.quickdeliver.repository.EtaPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PythonEtaClientService {

    @Value("${python.eta.service.url:http://localhost:5000}")
    private String pythonServiceUrl;

    @Autowired
    private EtaPredictionRepository etaPredictionRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Calls the Python Regression Microservice to compute predicted delivery time in minutes.
     * Persists the prediction in MySQL eta_predictions table.
     */
    public EtaPrediction calculateAndSaveEta(Order order) {
        BigDecimal distance = order.getDistanceKm() != null ? order.getDistanceKm() : BigDecimal.valueOf(3.5);
        int currentHour = LocalDateTime.now().getHour();

        int etaMinutes;
        try {
            String endpoint = pythonServiceUrl + "/predict";
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("distance_km", distance.doubleValue());
            requestPayload.put("hour_of_day", currentHour);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<EtaResponseDto> response = restTemplate.postForEntity(endpoint, entity, EtaResponseDto.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getEtaMinutes() != null) {
                etaMinutes = response.getBody().getEtaMinutes();
            } else {
                etaMinutes = calculateLocalFallbackEta(distance.doubleValue(), currentHour);
            }
        } catch (Exception ex) {
            // Graceful fallback to formula if Python service is momentarily unreachable
            System.err.println("Warning: Python ETA service unavailable, using regression fallback: " + ex.getMessage());
            etaMinutes = calculateLocalFallbackEta(distance.doubleValue(), currentHour);
        }

        // Save or update in database
        Optional<EtaPrediction> existing = etaPredictionRepository.findByOrderId(order.getId());
        EtaPrediction prediction = existing.orElse(new EtaPrediction(order, distance, currentHour, etaMinutes));
        prediction.setPredictedMins(etaMinutes);
        prediction.setDistanceKm(distance);
        prediction.setHourOfDay(currentHour);
        return etaPredictionRepository.save(prediction);
    }

    private int calculateLocalFallbackEta(double distanceKm, int hour) {
        double trafficMultiplier = (12 <= hour && hour <= 14) ? 1.35 : (19 <= hour && hour <= 22) ? 1.50 : 1.05;
        double basePrep = 10.0;
        double transitTime = (distanceKm * 2.4) * trafficMultiplier;
        return (int) Math.max(10, Math.round(basePrep + transitTime));
    }
}
