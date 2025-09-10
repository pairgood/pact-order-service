package com.ecommerce.orderservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Service
public class ProductServiceClient {
    
    private final WebClient webClient;
    
    @Value("${services.product-service.url:http://localhost:8082}")
    private String productServiceUrl;
    
    public ProductServiceClient() {
        this.webClient = WebClient.builder().build();
    }
    
    // Constructor for testing with custom base URL
    public ProductServiceClient(String baseUrl) {
        this.webClient = WebClient.builder().build();
        this.productServiceUrl = baseUrl;
    }
    
    public OrderService.ProductDto getProduct(Long productId) {
        return webClient.get()
            .uri(productServiceUrl + "/api/products/" + productId)
            .retrieve()
            .bodyToMono(OrderService.ProductDto.class)
            .block();
    }
}