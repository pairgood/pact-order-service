package com.ecommerce.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.service.NotificationServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(PactConsumerTestExt.class)
class NotificationServicePactTest {

    @Pact(consumer = "order-service", provider = "notification-service")
    public V4Pact sendOrderConfirmationPact(PactDslWithProvider builder) {
        return builder
            .given("notification service is available")
            .uponReceiving("a request to send order confirmation")
            .path("/api/notifications/order-confirmation")
            .method("POST")
            .headers(Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json"
            ))
            .body(LambdaDsl.newJsonBody((body) -> body
                .numberType("orderId", 1)
                .numberType("userId", 1)
            ).build())
            .willRespondWith()
            .status(200)
            .toPact()
            .asV4Pact().get();
    }

    @Pact(consumer = "order-service", provider = "notification-service")
    public V4Pact sendOrderStatusUpdatePact(PactDslWithProvider builder) {
        return builder
            .given("notification service is available")
            .uponReceiving("a request to send order status update")
            .path("/api/notifications/order-status")
            .method("POST")
            .headers(Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json"
            ))
            .body(LambdaDsl.newJsonBody((body) -> body
                .numberType("orderId", 1)
                .numberType("userId", 1)
                .stringType("status", "SHIPPED")
            ).build())
            .willRespondWith()
            .status(200)
            .toPact()
            .asV4Pact().get();
    }

    @Pact(consumer = "order-service", provider = "notification-service")
    public V4Pact sendOrderCancellationPact(PactDslWithProvider builder) {
        return builder
            .given("notification service is available")
            .uponReceiving("a request to send order cancellation")
            .path("/api/notifications/order-cancellation")
            .method("POST")
            .headers(Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json"
            ))
            .body(LambdaDsl.newJsonBody((body) -> body
                .numberType("orderId", 1)
                .numberType("userId", 1)
            ).build())
            .willRespondWith()
            .status(200)
            .toPact()
            .asV4Pact().get();
    }

    @Test
    @PactTestFor(pactMethod = "sendOrderConfirmationPact")
    void testSendOrderConfirmation(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        NotificationServiceClient client = createNotificationServiceClient(mockServer.getUrl());
        
        // Act: Send order confirmation
        assertThatNoException().isThrownBy(() -> {
            client.sendOrderConfirmation(1L, 1L);
        });
    }

    @Test
    @PactTestFor(pactMethod = "sendOrderStatusUpdatePact")
    void testSendOrderStatusUpdate(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        NotificationServiceClient client = createNotificationServiceClient(mockServer.getUrl());
        
        // Act: Send order status update
        assertThatNoException().isThrownBy(() -> {
            client.sendOrderStatusUpdate(1L, 1L, "SHIPPED");
        });
    }

    @Test
    @PactTestFor(pactMethod = "sendOrderCancellationPact")
    void testSendOrderCancellation(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        NotificationServiceClient client = createNotificationServiceClient(mockServer.getUrl());
        
        // Act: Send order cancellation
        assertThatNoException().isThrownBy(() -> {
            client.sendOrderCancellation(1L, 1L);
        });
    }

    private NotificationServiceClient createNotificationServiceClient(String baseUrl) {
        return new NotificationServiceClient(baseUrl);
    }
}