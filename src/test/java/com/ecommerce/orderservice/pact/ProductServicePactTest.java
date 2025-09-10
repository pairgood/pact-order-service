package com.ecommerce.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.ProductServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
class ProductServicePactTest {

    @Pact(consumer = "order-service", provider = "product-service")
    public RequestResponsePact getProductPact(PactDslWithProvider builder) {
        return builder
            .given("product 1 exists")
            .uponReceiving("a request to get product 1")
            .path("/api/products/1")
            .method("GET")
            .headers(Map.of(
                "Accept", "application/json"
            ))
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(LambdaDsl.newJsonBody((body) -> body
                .numberType("id", 1)
                .stringType("name", "Test Product")
                .numberType("price", 99.99)
            ).build())
            .toPact();
    }

    @Pact(consumer = "order-service", provider = "product-service")
    public RequestResponsePact getProductNotFoundPact(PactDslWithProvider builder) {
        return builder
            .given("product 999 does not exist")
            .uponReceiving("a request to get product 999")
            .path("/api/products/999")
            .method("GET")
            .headers(Map.of(
                "Accept", "application/json"
            ))
            .willRespondWith()
            .status(404)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getProductPact")
    void testGetProduct(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        ProductServiceClient client = createProductServiceClient(mockServer.getUrl());
        
        // Act: Get product details
        OrderService.ProductDto product = client.getProduct(1L);
        
        // Assert: Product details should be returned
        assertThat(product).isNotNull();
        assertThat(product.getId()).isEqualTo(1L);
        assertThat(product.getName()).isEqualTo("Test Product");
        assertThat(product.getPrice()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    @PactTestFor(pactMethod = "getProductNotFoundPact")
    void testGetProductNotFound(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        ProductServiceClient client = createProductServiceClient(mockServer.getUrl());
        
        // Act & Assert: Should throw exception for non-existing product
        try {
            client.getProduct(999L);
            assertThat(false).as("Expected exception to be thrown").isTrue();
        } catch (Exception e) {
            // Expected behavior - product not found should throw exception
            assertThat(e).isNotNull();
        }
    }

    private ProductServiceClient createProductServiceClient(String baseUrl) {
        return new ProductServiceClient(baseUrl);
    }
}