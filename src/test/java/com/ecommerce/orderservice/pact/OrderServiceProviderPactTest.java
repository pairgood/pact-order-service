package com.ecommerce.orderservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.NotificationServiceClient;
import com.ecommerce.orderservice.service.ProductServiceClient;
import com.ecommerce.orderservice.service.UserServiceClient;
import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Provider("order-service")   // MUST match spring.application.name exactly
@PactBroker(
    url = "http://localhost:9292",
    authentication = @PactBrokerAuth(username = "admin", password = "admin")
)
@IgnoreNoPactsToVerify  // Allow test to pass when no consumer pacts exist yet
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceProviderPactTest {

    @LocalServerPort
    private int port;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    @MockBean
    private TelemetryClient telemetryClient;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        // Context will be null when @IgnoreNoPactsToVerify creates a placeholder test
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }

        // Set up default mock behavior for telemetry client
        when(telemetryClient.startTrace(anyString(), anyString(), anyString(), anyString())).thenReturn("trace_123");
        doNothing().when(telemetryClient).finishTrace(anyString(), anyInt(), anyString());
        doNothing().when(telemetryClient).logEvent(anyString(), anyString());

        // Set up default mock behavior for external service clients
        doNothing().when(notificationServiceClient).sendOrderConfirmation(anyLong(), anyLong());
        doNothing().when(notificationServiceClient).sendOrderStatusUpdate(anyLong(), anyLong(), anyString());
        doNothing().when(notificationServiceClient).sendOrderCancellation(anyLong(), anyLong());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        // Context will be null when @IgnoreNoPactsToVerify creates a placeholder test
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // State string must be IDENTICAL to consumer's given() — character for character
    @State("an order with id 1 exists")
    void orderWithId1Exists() {
        Order order = new Order(42L, new BigDecimal("99.99"), "123 Main St");
        order.setId(1L);
        order.setStatus(Order.OrderStatus.PENDING);

        OrderItem orderItem = new OrderItem(101L, "Test Product", 2, new BigDecimal("49.995"));
        orderItem.setId(1L);
        orderItem.setOrder(order);

        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(orderItem);
        order.setOrderItems(orderItems);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @State("an order with id 999 does not exist")
    void orderWithId999DoesNotExist() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
    }

    @State("user with id 42 exists and products are available")
    void userAndProductsExist() {
        // Mock user validation
        when(userServiceClient.validateUser(42L)).thenReturn(true);

        // Mock product service response
        com.ecommerce.orderservice.service.OrderService.ProductDto product =
            new com.ecommerce.orderservice.service.OrderService.ProductDto();
        product.setId(101L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("49.99"));

        when(productServiceClient.getProduct(anyLong())).thenReturn(product);

        // Mock order repository save
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
    }

    @State("orders exist for user 42")
    void ordersExistForUser42() {
        Order order1 = new Order(42L, new BigDecimal("99.99"), "123 Main St");
        order1.setId(1L);
        order1.setStatus(Order.OrderStatus.PENDING);

        Order order2 = new Order(42L, new BigDecimal("149.99"), "456 Oak Ave");
        order2.setId(2L);
        order2.setStatus(Order.OrderStatus.CONFIRMED);

        List<Order> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);

        when(orderRepository.findByUserId(42L)).thenReturn(orders);
    }

    @State("multiple orders exist in the system")
    void multipleOrdersExist() {
        Order order1 = new Order(42L, new BigDecimal("99.99"), "123 Main St");
        order1.setId(1L);
        order1.setStatus(Order.OrderStatus.PENDING);

        Order order2 = new Order(43L, new BigDecimal("149.99"), "456 Oak Ave");
        order2.setId(2L);
        order2.setStatus(Order.OrderStatus.SHIPPED);

        List<Order> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);

        when(orderRepository.findAll()).thenReturn(orders);
    }
}
