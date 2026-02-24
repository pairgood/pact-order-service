package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Service
public class ProductServiceClient {
    
    private final WebClient webClient;
    
    @Value("${services.product-service.url:http://localhost:8082}")
    private String productServiceUrl;
    
    @Autowired(required = false)
    private TelemetryClient telemetryClient;
    
    @Autowired
    public ProductServiceClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // Constructor for testing with custom base URL
    public ProductServiceClient(String baseUrl) {
        this.webClient = WebClient.builder().build();
        this.productServiceUrl = baseUrl;
    }
    
    public OrderService.ProductDto getProduct(Long productId) {
        long startTime = System.currentTimeMillis();
        String url = productServiceUrl + "/api/products/" + productId;
        int statusCode = 200;
        
        try {
            OrderService.ProductDto product = webClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(OrderService.ProductDto.class)
                .block();
            
            long duration = System.currentTimeMillis() - startTime;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("product-service", "get_product", "GET", url, duration, statusCode);
            }
            return product;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            statusCode = 500;
            if (telemetryClient != null) {
                telemetryClient.recordServiceCall("product-service", "get_product", "GET", url, duration, statusCode);
            }
            throw e;
        }
    }
}