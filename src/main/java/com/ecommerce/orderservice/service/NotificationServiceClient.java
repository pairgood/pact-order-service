package com.ecommerce.orderservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class NotificationServiceClient {
    
    private final WebClient webClient;
    
    @Value("${services.notification-service.url:http://localhost:8085}")
    private String notificationServiceUrl;
    
    public NotificationServiceClient() {
        this.webClient = WebClient.builder().build();
    }
    
    // Constructor for testing with custom base URL
    public NotificationServiceClient(String baseUrl) {
        this.webClient = WebClient.builder().build();
        this.notificationServiceUrl = baseUrl;
    }
    
    public void sendOrderConfirmation(Long orderId, Long userId) {
        try {
            webClient.post()
                .uri(notificationServiceUrl + "/api/notifications/order-confirmation")
                .bodyValue(Map.of("orderId", orderId, "userId", userId))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (Exception e) {
            // Log error but don't fail order creation
            System.err.println("Failed to send order confirmation notification: " + e.getMessage());
        }
    }
    
    public void sendOrderStatusUpdate(Long orderId, Long userId, String status) {
        try {
            webClient.post()
                .uri(notificationServiceUrl + "/api/notifications/order-status")
                .bodyValue(Map.of("orderId", orderId, "userId", userId, "status", status))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (Exception e) {
            System.err.println("Failed to send order status notification: " + e.getMessage());
        }
    }
    
    public void sendOrderCancellation(Long orderId, Long userId) {
        try {
            webClient.post()
                .uri(notificationServiceUrl + "/api/notifications/order-cancellation")
                .bodyValue(Map.of("orderId", orderId, "userId", userId))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (Exception e) {
            System.err.println("Failed to send order cancellation notification: " + e.getMessage());
        }
    }
}