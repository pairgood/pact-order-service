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
    
    @Autowired(required = false)
    private TelemetryClient telemetryClient;
    
    @Autowired
    public UserServiceClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // Constructor for testing with custom base URL
    public UserServiceClient(String baseUrl) {
        this.webClient = WebClient.builder().build();
        this.userServiceUrl = baseUrl;
    }
    
    public boolean validateUser(Long userId) {
        long startTime = System.currentTimeMillis();
        String url = userServiceUrl + "/api/users/" + userId;
        
        try {
            webClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
            
            long duration = System.currentTimeMillis() - startTime;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("user-service", "validate_user", "GET", url, duration, 200);
            }
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("user-service", "validate_user", "GET", url, duration, 404);
            }
            return false;
        }
    }
}