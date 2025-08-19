package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class UserServiceClient {
    
    private final WebClient webClient;
    
    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;
    
    @Autowired
    private TelemetryClient telemetryClient;
    
    public UserServiceClient() {
        this.webClient = WebClient.builder().build();
    }
    
    public boolean validateUser(Long userId) {
        long startTime = System.currentTimeMillis();
        String url = userServiceUrl + "/api/users/" + userId;
        
        try {
            webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
            
            long duration = System.currentTimeMillis() - startTime;
            telemetryClient.recordServiceCall("user-service", "validate_user", "GET", url, duration, 200);
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            telemetryClient.recordServiceCall("user-service", "validate_user", "GET", url, duration, 404);
            return false;
        }
    }
}