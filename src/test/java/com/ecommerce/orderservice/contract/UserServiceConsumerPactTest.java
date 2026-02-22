package com.ecommerce.orderservice.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.service.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "user-service")
class UserServiceConsumerPactTest {

    @Pact(consumer = "order-service", provider = "user-service")
    V4Pact validateExistingUser(PactBuilder builder) {
        return builder
            .given("a user with id 42 exists")
            .expectsToReceiveHttpInteraction("a request to validate user 42", interaction -> interaction
                .withRequest(request -> request
                    .method("GET")
                    .path("/api/users/42")
                    .header("Accept", "application/json")
                )
                .willRespondWith(response -> response
                    .status(200)
                    .body(LambdaDsl.newJsonBody(body -> {
                        body.integerType("id", 42);
                        body.stringType("username", "johndoe");
                        body.stringType("email", "john.doe@example.com");
                        body.stringType("password", "hashedPassword");
                        body.stringType("firstName", "John");
                        body.stringType("lastName", "Doe");
                        body.stringType("address", "123 Main St");
                        body.stringType("phoneNumber", "555-1234");
                    }).build())
                )
            )
            .toPact();
    }

    @Pact(consumer = "order-service", provider = "user-service")
    V4Pact validateNonExistentUser(PactBuilder builder) {
        return builder
            .given("a user with id 999 does not exist")
            .expectsToReceiveHttpInteraction("a request to validate user that does not exist", interaction -> interaction
                .withRequest(request -> request
                    .method("GET")
                    .path("/api/users/999")
                    .header("Accept", "application/json")
                )
                .willRespondWith(response -> response
                    .status(404)
                )
            )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "validateExistingUser")
    void shouldValidateExistingUser(MockServer mockServer) {
        UserServiceClient client = new UserServiceClient();
        ReflectionTestUtils.setField(client, "userServiceUrl", mockServer.getUrl());
        
        boolean result = client.validateUser(42L);
        
        assertThat(result).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "validateNonExistentUser")
    void shouldReturnFalseForNonExistentUser(MockServer mockServer) {
        UserServiceClient client = new UserServiceClient();
        ReflectionTestUtils.setField(client, "userServiceUrl", mockServer.getUrl());
        
        boolean result = client.validateUser(999L);
        
        assertThat(result).isFalse();
    }
}
