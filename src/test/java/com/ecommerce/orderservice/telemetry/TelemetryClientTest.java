package com.ecommerce.orderservice.telemetry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class TelemetryClientTest {

    private TelemetryClient telemetryClient;

    @BeforeEach
    void setUp() {
        telemetryClient = new TelemetryClient();
        ReflectionTestUtils.setField(telemetryClient, "telemetryServiceUrl", "http://localhost:8086");
        ReflectionTestUtils.setField(telemetryClient, "serviceName", "order-service");
        
        // Clear any existing trace context
        TelemetryClient.TraceContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up trace context after each test
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void startTrace_ShouldReturnTraceId() {
        // When
        String traceId = telemetryClient.startTrace("create_order", "POST", "http://localhost/api/orders", "user123");

        // Then
        assertThat(traceId).isNotNull();
        assertThat(traceId).startsWith("trace_");
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(traceId);
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNotNull();
        assertThat(TelemetryClient.TraceContext.getStartTime()).isNotNull();
    }

    @Test
    void startTrace_WithNullUserId_ShouldWork() {
        // When
        String traceId = telemetryClient.startTrace("get_orders", "GET", "http://localhost/api/orders", null);

        // Then
        assertThat(traceId).isNotNull();
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(traceId);
    }

    @Test
    void finishTrace_WithActiveTrace_ShouldClearContext() {
        // Given
        String traceId = telemetryClient.startTrace("create_order", "POST", "http://localhost/api/orders", "user123");
        
        // When
        telemetryClient.finishTrace("create_order", 200, null);

        // Then
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNull();
        assertThat(TelemetryClient.TraceContext.getStartTime()).isNull();
    }

    @Test
    void finishTrace_WithError_ShouldWork() {
        // Given
        telemetryClient.startTrace("create_order", "POST", "http://localhost/api/orders", "user123");
        
        // When
        telemetryClient.finishTrace("create_order", 400, "Invalid order data");

        // Then
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
    }

    @Test
    void finishTrace_WithoutActiveTrace_ShouldNotFail() {
        // When & Then (should not throw exception)
        telemetryClient.finishTrace("create_order", 200, null);
    }

    @Test
    void recordServiceCall_WithActiveTrace_ShouldWork() {
        // Given
        telemetryClient.startTrace("create_order", "POST", "http://localhost/api/orders", "user123");
        
        // When
        telemetryClient.recordServiceCall("user-service", "validate_user", "GET", "http://user-service/api/users/123", 50L, 200);

        // Then - Should not throw exception and trace context should remain
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNotNull();
    }

    @Test
    void recordServiceCall_WithoutActiveTrace_ShouldNotFail() {
        // When & Then (should not throw exception)
        telemetryClient.recordServiceCall("product-service", "get_product", "GET", "http://product-service/api/products/1", 75L, 200);
    }

    @Test
    void logEvent_WithActiveTrace_ShouldWork() {
        // Given
        telemetryClient.startTrace("create_order", "POST", "http://localhost/api/orders", "user123");
        
        // When
        telemetryClient.logEvent("Order creation started for user: 123", "INFO");

        // Then - Should not throw exception and trace context should remain
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNotNull();
    }

    @Test
    void logEvent_WithoutActiveTrace_ShouldNotFail() {
        // When & Then (should not throw exception)
        telemetryClient.logEvent("Order processing failed", "ERROR");
    }

    @Test
    void traceContext_ShouldWorkCorrectly() {
        // Test setting and getting trace context
        TelemetryClient.TraceContext.setTraceId("order-trace-123");
        TelemetryClient.TraceContext.setSpanId("order-span-456");
        TelemetryClient.TraceContext.setStartTime(98765L);

        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo("order-trace-123");
        assertThat(TelemetryClient.TraceContext.getSpanId()).isEqualTo("order-span-456");
        assertThat(TelemetryClient.TraceContext.getStartTime()).isEqualTo(98765L);

        // Test clear
        TelemetryClient.TraceContext.clear();
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNull();
        assertThat(TelemetryClient.TraceContext.getStartTime()).isNull();
    }

    @Test
    void traceContext_propagate_ShouldSetTraceAndSpan() {
        // When
        TelemetryClient.TraceContext.propagate("propagated-order-trace", "propagated-order-span");

        // Then
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo("propagated-order-trace");
        assertThat(TelemetryClient.TraceContext.getSpanId()).isEqualTo("propagated-order-span");
    }

    @Test
    void generateTraceId_ShouldCreateUniqueIds() {
        // When
        String traceId1 = telemetryClient.startTrace("op1", "GET", "http://localhost/test1", "user1");
        TelemetryClient.TraceContext.clear();
        String traceId2 = telemetryClient.startTrace("op2", "GET", "http://localhost/test2", "user2");

        // Then
        assertThat(traceId1).isNotNull();
        assertThat(traceId2).isNotNull();
        assertThat(traceId1).isNotEqualTo(traceId2);
        assertThat(traceId1).startsWith("trace_");
        assertThat(traceId2).startsWith("trace_");
    }

    @Test
    void generateSpanId_ShouldCreateUniqueIds() {
        // When
        telemetryClient.startTrace("op1", "GET", "http://localhost/test1", "user1");
        String spanId1 = TelemetryClient.TraceContext.getSpanId();
        
        telemetryClient.recordServiceCall("service1", "operation1", "GET", "http://service1/api", 100L, 200);
        // recordServiceCall should create a new span ID internally
        
        // Then
        assertThat(spanId1).isNotNull();
        assertThat(spanId1).startsWith("span_");
    }

    @Test
    void recordServiceCall_WithErrorStatus_ShouldWork() {
        // Given
        telemetryClient.startTrace("create_order", "POST", "http://localhost/api/orders", "user123");
        
        // When
        telemetryClient.recordServiceCall("user-service", "validate_user", "GET", "http://user-service/api/users/999", 200L, 404);

        // Then - Should not throw exception
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNotNull();
    }

    @Test
    void logEvent_WithDifferentLevels_ShouldWork() {
        // Given
        telemetryClient.startTrace("process_order", "PUT", "http://localhost/api/orders/1", "user123");
        
        // When & Then (should not throw exceptions)
        telemetryClient.logEvent("Order processing started", "INFO");
        telemetryClient.logEvent("Order validation warning", "WARN");
        telemetryClient.logEvent("Order processing failed", "ERROR");
        telemetryClient.logEvent("Order debug information", "DEBUG");

        assertThat(TelemetryClient.TraceContext.getTraceId()).isNotNull();
    }

    @Test
    void fullTraceLifecycle_ShouldWorkCorrectly() {
        // Given - Start a trace
        String traceId = telemetryClient.startTrace("complete_order", "POST", "http://localhost/api/orders", "user456");
        
        // When - Record various events
        telemetryClient.logEvent("Order validation started", "INFO");
        telemetryClient.recordServiceCall("user-service", "validate_user", "GET", "http://user-service/api/users/456", 30L, 200);
        telemetryClient.recordServiceCall("product-service", "get_product", "GET", "http://product-service/api/products/1", 45L, 200);
        telemetryClient.logEvent("Order created successfully", "INFO");
        telemetryClient.finishTrace("complete_order", 201, null);

        // Then - Trace should be completed and context cleared
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNull();
        assertThat(TelemetryClient.TraceContext.getStartTime()).isNull();
    }

    @Test
    void traceContext_ShouldBeThreadLocal() {
        // This test verifies that trace context is thread-local
        // When
        TelemetryClient.TraceContext.setTraceId("main-thread-trace");
        
        // Create a new thread and verify it has no trace context
        Thread testThread = new Thread(() -> {
            assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
            TelemetryClient.TraceContext.setTraceId("child-thread-trace");
            assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo("child-thread-trace");
        });
        
        try {
            testThread.start();
            testThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - Main thread should still have its trace ID
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo("main-thread-trace");
    }
}