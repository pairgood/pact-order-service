package com.ecommerce.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.service.NotificationServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "notification-service")
class NotificationServiceConsumerPactTest {

    @Pact(consumer = "order-service", provider = "notification-service")
    V4Pact sendOrderConfirmation(PactBuilder builder) {
        return builder
            .given("a user with id 42 exists")
            .expectsToReceiveHttpInteraction("a request to send order confirmation for order 100", interaction -> interaction
                .withRequest(request -> request
                    .method("POST")
                    .path("/api/notifications/order-confirmation")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(LambdaDsl.newJsonBody(body -> {
                        body.integerType("orderId", 100);
                        body.integerType("userId", 42);
                    }).build())
                )
                .willRespondWith(response -> response
                    .status(200)
                )
            )
            .toPact();
    }

    @Pact(consumer = "order-service", provider = "notification-service")
    V4Pact sendOrderStatusUpdate(PactBuilder builder) {
        return builder
            .given("a user with id 42 exists")
            .expectsToReceiveHttpInteraction("a request to send order status update for order 100", interaction -> interaction
                .withRequest(request -> request
                    .method("POST")
                    .path("/api/notifications/order-status")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(LambdaDsl.newJsonBody(body -> {
                        body.integerType("orderId", 100);
                        body.integerType("userId", 42);
                        body.stringType("status", "SHIPPED");
                    }).build())
                )
                .willRespondWith(response -> response
                    .status(200)
                )
            )
            .toPact();
    }

    @Pact(consumer = "order-service", provider = "notification-service")
    V4Pact sendOrderCancellation(PactBuilder builder) {
        return builder
            .given("a user with id 42 exists")
            .expectsToReceiveHttpInteraction("a request to send order cancellation for order 100", interaction -> interaction
                .withRequest(request -> request
                    .method("POST")
                    .path("/api/notifications/order-cancellation")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(LambdaDsl.newJsonBody(body -> {
                        body.integerType("orderId", 100);
                        body.integerType("userId", 42);
                    }).build())
                )
                .willRespondWith(response -> response
                    .status(200)
                )
            )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "sendOrderConfirmation")
    void shouldSendOrderConfirmation(MockServer mockServer) {
        NotificationServiceClient client = new NotificationServiceClient();
        ReflectionTestUtils.setField(client, "notificationServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(client, "webClient", WebClient.builder().build());
        
        assertDoesNotThrow(() -> client.sendOrderConfirmation(100L, 42L));
    }

    @Test
    @PactTestFor(pactMethod = "sendOrderStatusUpdate")
    void shouldSendOrderStatusUpdate(MockServer mockServer) {
        NotificationServiceClient client = new NotificationServiceClient();
        ReflectionTestUtils.setField(client, "notificationServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(client, "webClient", WebClient.builder().build());
        
        assertDoesNotThrow(() -> client.sendOrderStatusUpdate(100L, 42L, "SHIPPED"));
    }

    @Test
    @PactTestFor(pactMethod = "sendOrderCancellation")
    void shouldSendOrderCancellation(MockServer mockServer) {
        NotificationServiceClient client = new NotificationServiceClient();
        ReflectionTestUtils.setField(client, "notificationServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(client, "webClient", WebClient.builder().build());
        
        assertDoesNotThrow(() -> client.sendOrderCancellation(100L, 42L));
    }
}
