package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class NotificationServiceClient {
    
    private final WebClient webClient;
    
    @Value("${services.notification-service.url:http://localhost:8085}")
    private String notificationServiceUrl;
    
    @Autowired(required = false)
    private TelemetryClient telemetryClient;
    
    public NotificationServiceClient() {
        this.webClient = WebClient.builder().build();
    }
    
    // Constructor for testing with custom base URL
    public NotificationServiceClient(String baseUrl) {
        this.webClient = WebClient.builder().build();
        this.notificationServiceUrl = baseUrl;
    }
    
    public void sendOrderConfirmation(Long orderId, Long userId) {
        long startTime = System.currentTimeMillis();
        String url = notificationServiceUrl + "/api/notifications/order-confirmation";
        int statusCode = 200;
        
        try {
            webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .bodyValue(Map.of("orderId", orderId, "userId", userId))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            
            long duration = System.currentTimeMillis() - startTime;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("notification-service", "send_order_confirmation", "POST", url, duration, statusCode);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            statusCode = 500;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("notification-service", "send_order_confirmation", "POST", url, duration, statusCode);
            }
            // Log error but don't fail order creation
            System.err.println("Failed to send order confirmation notification: " + e.getMessage());
        }
    }
    
    public void sendOrderStatusUpdate(Long orderId, Long userId, String status) {
        long startTime = System.currentTimeMillis();
        String url = notificationServiceUrl + "/api/notifications/order-status";
        int statusCode = 200;
        
        try {
            webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .bodyValue(Map.of("orderId", orderId, "userId", userId, "status", status))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            
            long duration = System.currentTimeMillis() - startTime;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("notification-service", "send_order_status_update", "POST", url, duration, statusCode);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            statusCode = 500;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("notification-service", "send_order_status_update", "POST", url, duration, statusCode);
            }
            System.err.println("Failed to send order status notification: " + e.getMessage());
        }
    }
    
    public void sendOrderCancellation(Long orderId, Long userId) {
        long startTime = System.currentTimeMillis();
        String url = notificationServiceUrl + "/api/notifications/order-cancellation";
        int statusCode = 200;
        
        try {
            webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .bodyValue(Map.of("orderId", orderId, "userId", userId))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            
            long duration = System.currentTimeMillis() - startTime;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("notification-service", "send_order_cancellation", "POST", url, duration, statusCode);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            statusCode = 500;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("notification-service", "send_order_cancellation", "POST", url, duration, statusCode);
            }
            System.err.println("Failed to send order cancellation notification: " + e.getMessage());
        }
    }
}