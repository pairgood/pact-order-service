package com.ecommerce.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.service.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
class UserServicePactTest {

    @Pact(consumer = "order-service", provider = "user-service")
    public V4Pact validateUserExistsPact(PactDslWithProvider builder) {
        return builder
            .given("user 1 exists")
            .uponReceiving("a request to validate user 1")
            .path("/api/users/1")
            .method("GET")
            .headers(Map.of("Accept", "application/json"))
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body("{\"id\": 1, \"email\": \"user@example.com\"}")
            .toPact()
            .asV4Pact().get();
    }

    @Pact(consumer = "order-service", provider = "user-service")
    public V4Pact validateUserNotFoundPact(PactDslWithProvider builder) {
        return builder
            .given("user 999 does not exist")
            .uponReceiving("a request to validate user 999")
            .path("/api/users/999")
            .method("GET")
            .headers(Map.of("Accept", "application/json"))
            .willRespondWith()
            .status(404)
            .toPact()
            .asV4Pact().get();
    }

    @Test
    @PactTestFor(pactMethod = "validateUserExistsPact")
    void testValidateUserExists(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        UserServiceClient client = createUserServiceClient(mockServer.getUrl());
        
        // Act: Validate existing user
        boolean result = client.validateUser(1L);
        
        // Assert: User validation should succeed
        assertThat(result).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "validateUserNotFoundPact")
    void testValidateUserNotFound(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        UserServiceClient client = createUserServiceClient(mockServer.getUrl());
        
        // Act: Validate non-existing user
        boolean result = client.validateUser(999L);
        
        // Assert: User validation should fail
        assertThat(result).isFalse();
    }

    private UserServiceClient createUserServiceClient(String baseUrl) {
        return new UserServiceClient(baseUrl);
    }
}