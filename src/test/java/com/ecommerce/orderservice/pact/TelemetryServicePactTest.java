package com.ecommerce.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(PactConsumerTestExt.class)
class TelemetryServicePactTest {

    @Pact(consumer = "order-service", provider = "telemetry-service")
    public V4Pact sendTelemetryEventPact(PactDslWithProvider builder) {
        return builder
            .given("telemetry service is available")
            .uponReceiving("a request to send telemetry event")
            .path("/api/telemetry/events")
            .method("POST")
            .headers(Map.of(
                "Content-Type", "application/json",
                "Accept", "application/json"
            ))
            .body(LambdaDsl.newJsonBody((body) -> body
                .stringType("traceId")
                .stringType("spanId")
                .stringType("serviceName", "order-service")
                .stringType("operation")
                .stringType("eventType")
                .stringType("httpMethod")
                .stringType("httpUrl")
                .stringType("userId")
                .stringType("status")
                .array("timestamp", array -> array // LocalDateTime serializes as [year, month, day, hour, minute, second, nanosecond]
                    .numberType(2025)    // year
                    .numberType(1)       // month  
                    .numberType(1)       // day
                    .numberType(0)       // hour
                    .numberType(0)       // minute
                    .numberType(0)       // second
                    .numberType(0)       // nanosecond
                )
            ).build())
            .willRespondWith()
            .status(200)
            .toPact()
            .asV4Pact().get();
    }

    @Test
    @PactTestFor(pactMethod = "sendTelemetryEventPact")
    void testSendTelemetryEvent(MockServer mockServer) {
        // Arrange: Create client with mock server URL
        TelemetryClient client = createTelemetryClient(mockServer.getUrl());
        
        // Act: Send telemetry event (this method is async, so we just verify no exception)
        assertThatNoException().isThrownBy(() -> {
            client.startTrace("test_operation", "GET", "/test", "user123");
            // Give some time for the async operation to complete
            Thread.sleep(100);
        });
    }

    private TelemetryClient createTelemetryClient(String baseUrl) {
        return new TelemetryClient(baseUrl, "order-service");
    }
}