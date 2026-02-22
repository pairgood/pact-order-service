package com.ecommerce.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.ProductServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "product-service")
class ProductServiceConsumerPactTest {

    @Pact(consumer = "order-service", provider = "product-service")
    V4Pact getExistingProduct(PactBuilder builder) {
        return builder
            .given("a product with id 42 exists")
            .expectsToReceiveHttpInteraction("a request to get product 42", interaction -> interaction
                .withRequest(request -> request
                    .method("GET")
                    .path("/api/products/42")
                    .header("Accept", "application/json")
                )
                .willRespondWith(response -> response
                    .status(200)
                    .body(LambdaDsl.newJsonBody(body -> {
                        body.integerType("id", 42);
                        body.stringType("name", "Widget");
                        body.numberType("price", 99.99);
                    }).build())
                )
            )
            .toPact();
    }

    @Pact(consumer = "order-service", provider = "product-service")
    V4Pact getNonExistentProduct(PactBuilder builder) {
        return builder
            .given("a product with id 999 does not exist")
            .expectsToReceiveHttpInteraction("a request for product that does not exist", interaction -> interaction
                .withRequest(request -> request
                    .method("GET")
                    .path("/api/products/999")
                    .header("Accept", "application/json")
                )
                .willRespondWith(response -> response
                    .status(500)
                )
            )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getExistingProduct")
    void shouldGetExistingProduct(MockServer mockServer) {
        ProductServiceClient client = new ProductServiceClient();
        ReflectionTestUtils.setField(client, "productServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(client, "webClient", WebClient.builder().build());
        
        OrderService.ProductDto product = client.getProduct(42L);
        
        assertThat(product).isNotNull();
        assertThat(product.getId()).isEqualTo(42L);
        assertThat(product.getName()).isEqualTo("Widget");
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    @PactTestFor(pactMethod = "getNonExistentProduct")
    void shouldThrowExceptionForNonExistentProduct(MockServer mockServer) {
        ProductServiceClient client = new ProductServiceClient();
        ReflectionTestUtils.setField(client, "productServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(client, "webClient", WebClient.builder().build());
        
        assertThatThrownBy(() -> client.getProduct(999L))
            .isInstanceOf(WebClientResponseException.class)
            .matches(e -> ((WebClientResponseException) e).getStatusCode().value() == 500);
    }
}
