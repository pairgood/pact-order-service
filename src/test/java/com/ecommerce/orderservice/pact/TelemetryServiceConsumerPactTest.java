package com.ecommerce.orderservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "telemetry-service")
class TelemetryServiceConsumerPactTest {

    private TelemetryClient telemetryClient;

    @BeforeEach
    void setUp() {
        telemetryClient = new TelemetryClient();
        ReflectionTestUtils.setField(telemetryClient, "serviceName", "order-service");
        TelemetryClient.TraceContext.clear();
    }

    @AfterEach
    void tearDown() {
        TelemetryClient.TraceContext.clear();
    }

    @Pact(consumer = "order-service", provider = "telemetry-service")
    public V4Pact startTraceInteraction(PactBuilder builder) {
        return builder
                .given("the telemetry service is available to accept events")
                .expectsToReceiveHttpInteraction("a start trace event", interaction -> interaction
                    .withRequest(request -> request
                        .method("POST")
                        .path("/api/telemetry/events")
                        .body(LambdaDsl.newJsonBody(body -> {
                            body.stringType("traceId", "trace_example");
                            body.stringType("spanId", "span_example");
                            body.stringValue("serviceName", "order-service");
                            body.stringValue("operation", "create_order");
                            body.stringValue("eventType", "SPAN");
                            body.minArrayLike("timestamp", 7, PactDslJsonRootValue.integerType(2024), 7);
                            body.stringValue("status", "SUCCESS");
                            body.stringValue("httpMethod", "POST");
                            body.stringValue("httpUrl", "/api/orders");
                            body.stringValue("userId", "42");
                        }).build())
                    )
                    .willRespondWith(response -> response
                        .status(200)
                    )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "startTraceInteraction")
    void testStartTrace(MockServer mockServer) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Configure TelemetryClient to use mock server
        ReflectionTestUtils.setField(telemetryClient, "telemetryServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(telemetryClient, "webClient", WebClient.builder().build());
        
        // Call startTrace
        String traceId = telemetryClient.startTrace(
            "create_order", 
            "POST", 
            "/api/orders", 
            "42"
        );

        // Wait for async call with timeout
        latch.await(2, TimeUnit.SECONDS);

        // Verify trace context was set
        assertThat(traceId).isNotNull();
        assertThat(traceId).startsWith("trace_");
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(traceId);
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNotNull();
    }

    @Pact(consumer = "order-service", provider = "telemetry-service")
    public V4Pact finishTraceInteraction(PactBuilder builder) {
        return builder
                .given("the telemetry service is available to accept events")
                .expectsToReceiveHttpInteraction("a finish trace event with success", interaction -> interaction
                    .withRequest(request -> request
                        .method("POST")
                        .path("/api/telemetry/events")
                        .body(LambdaDsl.newJsonBody(body -> {
                            body.stringType("traceId", "trace_example");
                            body.stringType("spanId", "span_example");
                            body.stringValue("serviceName", "order-service");
                            body.stringValue("operation", "create_order_complete");
                            body.stringValue("eventType", "SPAN");
                            body.minArrayLike("timestamp", 7, PactDslJsonRootValue.integerType(2024), 7);
                            body.numberType("durationMs", 150);
                            body.stringValue("status", "SUCCESS");
                            body.numberType("httpStatusCode", 200);
                            body.stringValue("errorMessage", "");
                        }).build())
                    )
                    .willRespondWith(response -> response
                        .status(200)
                    )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "finishTraceInteraction")
    void testFinishTrace(MockServer mockServer) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Configure TelemetryClient to use mock server
        ReflectionTestUtils.setField(telemetryClient, "telemetryServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(telemetryClient, "webClient", WebClient.builder().build());

        // Set up trace context
        TelemetryClient.TraceContext.setTraceId("trace_abc123");
        TelemetryClient.TraceContext.setSpanId("span_def456");
        TelemetryClient.TraceContext.setStartTime(System.currentTimeMillis() - 150);

        // Call finishTrace
        telemetryClient.finishTrace("create_order", 200, null);

        // Wait for async call with timeout
        latch.await(2, TimeUnit.SECONDS);

        // Verify trace context was cleared
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNull();
    }

    @Pact(consumer = "order-service", provider = "telemetry-service")
    public V4Pact recordServiceCallInteraction(PactBuilder builder) {
        return builder
                .given("the telemetry service is available to accept events")
                .expectsToReceiveHttpInteraction("a service call event", interaction -> interaction
                    .withRequest(request -> request
                        .method("POST")
                        .path("/api/telemetry/events")
                        .body(LambdaDsl.newJsonBody(body -> {
                            body.stringType("traceId", "trace_example");
                            body.stringType("spanId", "span_example");
                            body.stringType("parentSpanId", "parent_span_example");
                            body.stringValue("serviceName", "order-service");
                            body.stringValue("operation", "user-service_validate_user");
                            body.stringValue("eventType", "SPAN");
                            body.minArrayLike("timestamp", 7, PactDslJsonRootValue.integerType(2024), 7);
                            body.numberType("durationMs", 120);
                            body.stringValue("status", "SUCCESS");
                            body.stringValue("httpMethod", "GET");
                            body.stringValue("httpUrl", "http://localhost:8081/api/users/42");
                            body.numberType("httpStatusCode", 200);
                            body.stringValue("metadata", "Outbound call to user-service");
                        }).build())
                    )
                    .willRespondWith(response -> response
                        .status(200)
                    )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "recordServiceCallInteraction")
    void testRecordServiceCall(MockServer mockServer) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Configure TelemetryClient to use mock server
        ReflectionTestUtils.setField(telemetryClient, "telemetryServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(telemetryClient, "webClient", WebClient.builder().build());

        // Set up trace context
        TelemetryClient.TraceContext.setTraceId("trace_abc123");
        TelemetryClient.TraceContext.setSpanId("span_def456");

        // Call recordServiceCall
        telemetryClient.recordServiceCall(
            "user-service",
            "validate_user",
            "GET",
            "http://localhost:8081/api/users/42",
            120,
            200
        );

        // Wait for async call with timeout
        latch.await(2, TimeUnit.SECONDS);

        // Verify trace context still exists (not cleared)
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo("trace_abc123");
        assertThat(TelemetryClient.TraceContext.getSpanId()).isEqualTo("span_def456");
    }

    @Pact(consumer = "order-service", provider = "telemetry-service")
    public V4Pact telemetryServiceUnavailable(PactBuilder builder) {
        return builder
                .given("the telemetry service is unavailable")
                .expectsToReceiveHttpInteraction("a telemetry event when service returns 500", interaction -> interaction
                    .withRequest(request -> request
                        .method("POST")
                        .path("/api/telemetry/events")
                        .body(LambdaDsl.newJsonBody(body -> {
                            body.stringType("traceId", "trace_example");
                            body.stringType("spanId", "span_example");
                            body.stringValue("serviceName", "order-service");
                            body.stringValue("operation", "process_order");
                            body.stringValue("eventType", "SPAN");
                            body.minArrayLike("timestamp", 7, PactDslJsonRootValue.integerType(2024), 7);
                            body.stringValue("status", "SUCCESS");
                            body.stringValue("httpMethod", "POST");
                            body.stringValue("httpUrl", "/api/orders");
                            body.stringValue("userId", "");
                        }).build())
                    )
                    .willRespondWith(response -> response
                        .status(500)
                    )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "telemetryServiceUnavailable")
    void testTelemetryServiceUnavailable(MockServer mockServer) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Configure TelemetryClient to use mock server
        ReflectionTestUtils.setField(telemetryClient, "telemetryServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(telemetryClient, "webClient", WebClient.builder().build());
        
        // Call startTrace - should handle 500 gracefully without throwing
        String traceId = telemetryClient.startTrace(
            "process_order", 
            "POST", 
            "/api/orders", 
            null
        );

        // Wait for async call with timeout
        latch.await(2, TimeUnit.SECONDS);

        // Verify trace context was still set despite service error
        assertThat(traceId).isNotNull();
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(traceId);
    }

    @Pact(consumer = "order-service", provider = "telemetry-service")
    public V4Pact finishTraceWithError(PactBuilder builder) {
        return builder
                .given("the telemetry service is available to accept events")
                .expectsToReceiveHttpInteraction("a finish trace event with error status", interaction -> interaction
                    .withRequest(request -> request
                        .method("POST")
                        .path("/api/telemetry/events")
                        .body(LambdaDsl.newJsonBody(body -> {
                            body.stringType("traceId", "trace_example");
                            body.stringType("spanId", "span_example");
                            body.stringValue("serviceName", "order-service");
                            body.stringValue("operation", "create_order_complete");
                            body.stringValue("eventType", "SPAN");
                            body.minArrayLike("timestamp", 7, PactDslJsonRootValue.integerType(2024), 7);
                            body.numberType("durationMs", 250);
                            body.stringValue("status", "ERROR");
                            body.numberType("httpStatusCode", 500);
                            body.stringValue("errorMessage", "Internal server error");
                        }).build())
                    )
                    .willRespondWith(response -> response
                        .status(200)
                    )
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "finishTraceWithError")
    void testFinishTraceWithError(MockServer mockServer) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Configure TelemetryClient to use mock server
        ReflectionTestUtils.setField(telemetryClient, "telemetryServiceUrl", mockServer.getUrl());
        ReflectionTestUtils.setField(telemetryClient, "webClient", WebClient.builder().build());

        // Set up trace context
        TelemetryClient.TraceContext.setTraceId("trace_error123");
        TelemetryClient.TraceContext.setSpanId("span_error456");
        TelemetryClient.TraceContext.setStartTime(System.currentTimeMillis() - 250);

        // Call finishTrace with error status
        telemetryClient.finishTrace("create_order", 500, "Internal server error");

        // Wait for async call with timeout
        latch.await(2, TimeUnit.SECONDS);

        // Verify trace context was cleared
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNull();
    }
}
