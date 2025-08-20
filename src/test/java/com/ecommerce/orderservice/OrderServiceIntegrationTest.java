package com.ecommerce.orderservice;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.UserServiceClient;
import com.ecommerce.orderservice.service.ProductServiceClient;
import com.ecommerce.orderservice.service.NotificationServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    private OrderRequest testOrderRequest;
    private OrderService.ProductDto testProduct1;
    private OrderService.ProductDto testProduct2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        // Setup test products
        testProduct1 = new OrderService.ProductDto();
        testProduct1.setId(1L);
        testProduct1.setName("Gaming Mouse");
        testProduct1.setPrice(new BigDecimal("49.99"));

        testProduct2 = new OrderService.ProductDto();
        testProduct2.setId(2L);
        testProduct2.setName("Keyboard");
        testProduct2.setPrice(new BigDecimal("99.99"));

        // Setup test order request
        testOrderRequest = new OrderRequest();
        testOrderRequest.setUserId(123L);
        testOrderRequest.setShippingAddress("123 Test St, Test City, TC 12345");

        OrderRequest.OrderItemRequest itemRequest1 = new OrderRequest.OrderItemRequest();
        itemRequest1.setProductId(1L);
        itemRequest1.setQuantity(1);

        OrderRequest.OrderItemRequest itemRequest2 = new OrderRequest.OrderItemRequest();
        itemRequest2.setProductId(2L);
        itemRequest2.setQuantity(2);

        testOrderRequest.setItems(Arrays.asList(itemRequest1, itemRequest2));

        // Setup mock responses
        when(userServiceClient.validateUser(anyLong())).thenReturn(true);
        when(productServiceClient.getProduct(1L)).thenReturn(testProduct1);
        when(productServiceClient.getProduct(2L)).thenReturn(testProduct2);
        doNothing().when(notificationServiceClient).sendOrderConfirmation(anyLong(), anyLong());
        doNothing().when(notificationServiceClient).sendOrderStatusUpdate(anyLong(), anyLong(), anyString());
        doNothing().when(notificationServiceClient).sendOrderCancellation(anyLong(), anyLong());
    }

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
        assertThat(orderService).isNotNull();
        assertThat(orderRepository).isNotNull();
        assertThat(userServiceClient).isNotNull();
        assertThat(productServiceClient).isNotNull();
        assertThat(notificationServiceClient).isNotNull();
    }

    @Test
    void fullOrderLifecycle_ShouldWork() {
        // Create order
        Order createdOrder = orderService.createOrder(testOrderRequest);
        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getUserId()).isEqualTo(123L);
        assertThat(createdOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(createdOrder.getTotalAmount()).isEqualTo(new BigDecimal("249.97")); // 49.99 + (99.99 * 2)
        assertThat(createdOrder.getOrderItems()).hasSize(2);

        // Verify order exists in database
        Order foundOrder = orderRepository.findById(createdOrder.getId()).orElse(null);
        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getUserId()).isEqualTo(123L);
        assertThat(foundOrder.getOrderItems()).hasSize(2);

        // Get order by ID
        Order retrievedOrder = orderService.getOrderById(createdOrder.getId());
        assertThat(retrievedOrder.getId()).isEqualTo(createdOrder.getId());
        assertThat(retrievedOrder.getUserId()).isEqualTo(123L);

        // Update order status
        Order confirmedOrder = orderService.updateOrderStatus(createdOrder.getId(), Order.OrderStatus.CONFIRMED);
        assertThat(confirmedOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);

        // Verify status persisted
        Order statusUpdatedOrder = orderRepository.findById(createdOrder.getId()).orElse(null);
        assertThat(statusUpdatedOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);

        // Get orders by user ID
        List<Order> userOrders = orderService.getOrdersByUserId(123L);
        assertThat(userOrders).hasSize(1);
        assertThat(userOrders.get(0).getId()).isEqualTo(createdOrder.getId());

        // Get all orders
        List<Order> allOrders = orderService.getAllOrders();
        assertThat(allOrders).hasSize(1);
        assertThat(allOrders.get(0).getId()).isEqualTo(createdOrder.getId());

        // Cancel order
        orderService.cancelOrder(createdOrder.getId());
        Order cancelledOrder = orderRepository.findById(createdOrder.getId()).orElse(null);
        assertThat(cancelledOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);

        // Verify notifications were sent
        verify(notificationServiceClient).sendOrderConfirmation(createdOrder.getId(), 123L);
        verify(notificationServiceClient).sendOrderStatusUpdate(createdOrder.getId(), 123L, "CONFIRMED");
        verify(notificationServiceClient).sendOrderCancellation(createdOrder.getId(), 123L);
    }

    @Test
    void createOrder_WithInvalidUser_ShouldThrowException() {
        // Given
        when(userServiceClient.validateUser(123L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.createOrder(testOrderRequest));
        
        assertThat(exception.getMessage()).isEqualTo("User not found");
        
        // Verify no order was created
        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    void orderRepository_ShouldSupportCustomQueries() {
        // Create test orders for different users and statuses
        Order order1 = new Order(100L, new BigDecimal("99.99"), "Address 1");
        order1.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(order1);

        Order order2 = new Order(100L, new BigDecimal("199.99"), "Address 2");
        order2.setStatus(Order.OrderStatus.DELIVERED);
        orderRepository.save(order2);

        Order order3 = new Order(200L, new BigDecimal("299.99"), "Address 3");
        order3.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(order3);

        // Test findByUserId
        List<Order> user100Orders = orderRepository.findByUserId(100L);
        assertThat(user100Orders).hasSize(2);
        assertThat(user100Orders).extracting(Order::getUserId).containsOnly(100L);

        List<Order> user200Orders = orderRepository.findByUserId(200L);
        assertThat(user200Orders).hasSize(1);
        assertThat(user200Orders.get(0).getUserId()).isEqualTo(200L);

        // Test findByStatus
        List<Order> pendingOrders = orderRepository.findByStatus(Order.OrderStatus.PENDING);
        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).extracting(Order::getStatus).containsOnly(Order.OrderStatus.PENDING);

        List<Order> deliveredOrders = orderRepository.findByStatus(Order.OrderStatus.DELIVERED);
        assertThat(deliveredOrders).hasSize(1);
        assertThat(deliveredOrders.get(0).getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);

        // Test with non-existent data
        assertThat(orderRepository.findByUserId(999L)).isEmpty();
        assertThat(orderRepository.findByStatus(Order.OrderStatus.SHIPPED)).isEmpty();
    }

    @Test
    void orderService_ShouldCalculateTotalAmountCorrectly() {
        // Create order with multiple items and quantities
        OrderRequest.OrderItemRequest item1 = new OrderRequest.OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(3); // 3 * 49.99 = 149.97

        OrderRequest.OrderItemRequest item2 = new OrderRequest.OrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(2); // 2 * 99.99 = 199.98

        testOrderRequest.setItems(Arrays.asList(item1, item2));

        // Create order
        Order createdOrder = orderService.createOrder(testOrderRequest);

        // Verify total amount calculation
        BigDecimal expectedTotal = new BigDecimal("349.95"); // 149.97 + 199.98
        assertThat(createdOrder.getTotalAmount()).isEqualTo(expectedTotal);

        // Verify individual item totals
        OrderItem orderItem1 = createdOrder.getOrderItems().stream()
            .filter(item -> item.getProductId().equals(1L))
            .findFirst().orElse(null);
        assertThat(orderItem1).isNotNull();
        assertThat(orderItem1.getTotalPrice()).isEqualTo(new BigDecimal("149.97"));

        OrderItem orderItem2 = createdOrder.getOrderItems().stream()
            .filter(item -> item.getProductId().equals(2L))
            .findFirst().orElse(null);
        assertThat(orderItem2).isNotNull();
        assertThat(orderItem2.getTotalPrice()).isEqualTo(new BigDecimal("199.98"));
    }

    @Test
    void orderService_ShouldHandleStatusTransitions() {
        // Create initial order
        Order order = orderService.createOrder(testOrderRequest);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        // Test all status transitions
        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);

        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);

        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);

        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);

        // Verify status persisted
        Order persistedOrder = orderRepository.findById(order.getId()).orElse(null);
        assertThat(persistedOrder.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
    }

    @Test
    void orderService_ShouldHandleMultipleOrdersPerUser() {
        // Create multiple orders for same user
        Order order1 = orderService.createOrder(testOrderRequest);
        
        // Create second order
        OrderRequest secondRequest = new OrderRequest();
        secondRequest.setUserId(123L);
        secondRequest.setShippingAddress("456 Another St, Another City, AC 67890");
        
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(1);
        secondRequest.setItems(Arrays.asList(itemRequest));
        
        Order order2 = orderService.createOrder(secondRequest);

        // Verify both orders exist
        assertThat(order1.getId()).isNotEqualTo(order2.getId());
        assertThat(orderRepository.count()).isEqualTo(2);

        // Get orders by user ID
        List<Order> userOrders = orderService.getOrdersByUserId(123L);
        assertThat(userOrders).hasSize(2);
        assertThat(userOrders).extracting(Order::getUserId).containsOnly(123L);
        assertThat(userOrders).extracting(Order::getId).containsExactlyInAnyOrder(order1.getId(), order2.getId());
    }

    @Test
    void orderItems_ShouldHaveCorrectOrderReference() {
        // Create order
        Order createdOrder = orderService.createOrder(testOrderRequest);

        // Verify all order items have correct order reference
        for (OrderItem item : createdOrder.getOrderItems()) {
            assertThat(item.getOrder()).isEqualTo(createdOrder);
            assertThat(item.getProductId()).isNotNull();
            assertThat(item.getProductName()).isNotNull();
            assertThat(item.getQuantity()).isGreaterThan(0);
            assertThat(item.getUnitPrice()).isGreaterThan(BigDecimal.ZERO);
            assertThat(item.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Test
    void orderService_ShouldValidateOrderExists() {
        // Test getting non-existent order
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> orderService.getOrderById(999L));
        assertThat(exception.getMessage()).isEqualTo("Order not found");

        // Test updating non-existent order
        exception = assertThrows(RuntimeException.class,
            () -> orderService.updateOrderStatus(999L, Order.OrderStatus.CONFIRMED));
        assertThat(exception.getMessage()).isEqualTo("Order not found");

        // Test cancelling non-existent order
        exception = assertThrows(RuntimeException.class,
            () -> orderService.cancelOrder(999L));
        assertThat(exception.getMessage()).isEqualTo("Order not found");
    }
}