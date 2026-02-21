package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderRequest testOrderRequest;
    private OrderService.ProductDto testProduct1;
    private OrderService.ProductDto testProduct2;

    @BeforeEach
    void setUp() {
        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(123L);
        testOrder.setTotalAmount(new BigDecimal("149.98"));
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setShippingAddress("123 Test St, Test City, TC 12345");

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
        itemRequest2.setQuantity(1);

        testOrderRequest.setItems(Arrays.asList(itemRequest1, itemRequest2));
    }

    @Test
    void createOrder_WithValidRequest_ShouldReturnCreatedOrder() {
        // Given
        when(userServiceClient.validateUser(123L)).thenReturn(true);
        when(productServiceClient.getProduct(1L)).thenReturn(testProduct1);
        when(productServiceClient.getProduct(2L)).thenReturn(testProduct2);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(notificationServiceClient).sendOrderConfirmation(anyLong(), anyLong());

        // When
        Order result = orderService.createOrder(testOrderRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(123L);
        
        verify(userServiceClient).validateUser(123L);
        verify(productServiceClient).getProduct(1L);
        verify(productServiceClient).getProduct(2L);
        verify(orderRepository).save(any(Order.class));
        verify(notificationServiceClient).sendOrderConfirmation(1L, 123L);
    }

    @Test
    void createOrder_WithInvalidUser_ShouldThrowException() {
        // Given
        when(userServiceClient.validateUser(123L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.createOrder(testOrderRequest));
        
        assertThat(exception.getMessage()).isEqualTo("User not found");
        
        verify(userServiceClient).validateUser(123L);
        verify(productServiceClient, never()).getProduct(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationServiceClient, never()).sendOrderConfirmation(anyLong(), anyLong());
    }

    @Test
    void createOrder_ShouldCalculateTotalAmountCorrectly() {
        // Given
        when(userServiceClient.validateUser(123L)).thenReturn(true);
        when(productServiceClient.getProduct(1L)).thenReturn(testProduct1);
        when(productServiceClient.getProduct(2L)).thenReturn(testProduct2);
        
        // Capture the order being saved
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        
        doNothing().when(notificationServiceClient).sendOrderConfirmation(anyLong(), anyLong());

        // When
        Order result = orderService.createOrder(testOrderRequest);

        // Then
        verify(orderRepository).save(argThat(order -> {
            BigDecimal expectedTotal = new BigDecimal("49.99").add(new BigDecimal("99.99"));
            return order.getTotalAmount().compareTo(expectedTotal) == 0;
        }));
    }

    @Test
    void createOrder_ShouldSetOrderItemsCorrectly() {
        // Given
        when(userServiceClient.validateUser(123L)).thenReturn(true);
        when(productServiceClient.getProduct(1L)).thenReturn(testProduct1);
        when(productServiceClient.getProduct(2L)).thenReturn(testProduct2);
        
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        
        doNothing().when(notificationServiceClient).sendOrderConfirmation(anyLong(), anyLong());

        // When
        orderService.createOrder(testOrderRequest);

        // Then
        verify(orderRepository).save(argThat(order -> {
            List<OrderItem> items = order.getOrderItems();
            return items.size() == 2 &&
                   items.stream().anyMatch(item -> 
                       item.getProductId().equals(1L) && 
                       item.getProductName().equals("Gaming Mouse") &&
                       item.getQuantity().equals(1) &&
                       item.getUnitPrice().compareTo(new BigDecimal("49.99")) == 0) &&
                   items.stream().anyMatch(item -> 
                       item.getProductId().equals(2L) && 
                       item.getProductName().equals("Keyboard") &&
                       item.getQuantity().equals(1) &&
                       item.getUnitPrice().compareTo(new BigDecimal("99.99")) == 0);
        }));
    }

    @Test
    void getOrderById_WithExistingOrder_ShouldReturnOrder() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Order result = orderService.getOrderById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(123L);
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.getOrderById(999L));
        
        assertThat(exception.getMessage()).isEqualTo("Order not found");
        verify(orderRepository).findById(999L);
    }

    @Test
    void getOrdersByUserId_ShouldReturnUserOrders() {
        // Given
        Order order2 = new Order();
        order2.setId(2L);
        order2.setUserId(123L);
        
        List<Order> userOrders = Arrays.asList(testOrder, order2);
        when(orderRepository.findByUserId(123L)).thenReturn(userOrders);

        // When
        List<Order> result = orderService.getOrdersByUserId(123L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Order::getUserId).containsOnly(123L);
        verify(orderRepository).findByUserId(123L);
    }

    @Test
    void getAllOrders_ShouldReturnAllOrders() {
        // Given
        Order order2 = new Order();
        order2.setId(2L);
        order2.setUserId(456L);
        
        List<Order> allOrders = Arrays.asList(testOrder, order2);
        when(orderRepository.findAll()).thenReturn(allOrders);

        // When
        List<Order> result = orderService.getAllOrders();

        // Then
        assertThat(result).hasSize(2);
        verify(orderRepository).findAll();
    }

    @Test
    void updateOrderStatus_WithExistingOrder_ShouldUpdateAndNotify() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(notificationServiceClient).sendOrderStatusUpdate(anyLong(), anyLong(), anyString());

        // When
        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(argThat(order -> 
            order.getStatus() == Order.OrderStatus.CONFIRMED));
        verify(notificationServiceClient).sendOrderStatusUpdate(1L, 123L, "CONFIRMED");
    }

    @Test
    void updateOrderStatus_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.updateOrderStatus(999L, Order.OrderStatus.CONFIRMED));
        
        assertThat(exception.getMessage()).isEqualTo("Order not found");
        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationServiceClient, never()).sendOrderStatusUpdate(anyLong(), anyLong(), anyString());
    }

    @Test
    void cancelOrder_WithExistingOrder_ShouldCancelAndNotify() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(notificationServiceClient).sendOrderCancellation(anyLong(), anyLong());

        // When
        orderService.cancelOrder(1L);

        // Then
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(argThat(order -> 
            order.getStatus() == Order.OrderStatus.CANCELLED));
        verify(notificationServiceClient).sendOrderCancellation(1L, 123L);
    }

    @Test
    void cancelOrder_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> orderService.cancelOrder(999L));
        
        assertThat(exception.getMessage()).isEqualTo("Order not found");
        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationServiceClient, never()).sendOrderCancellation(anyLong(), anyLong());
    }

    @Test
    void createOrder_ShouldHandleMultipleQuantities() {
        // Given
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(3);
        testOrderRequest.setItems(Arrays.asList(itemRequest));

        when(userServiceClient.validateUser(123L)).thenReturn(true);
        when(productServiceClient.getProduct(1L)).thenReturn(testProduct1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        doNothing().when(notificationServiceClient).sendOrderConfirmation(anyLong(), anyLong());

        // When
        orderService.createOrder(testOrderRequest);

        // Then
        verify(orderRepository).save(argThat(order -> {
            OrderItem item = order.getOrderItems().get(0);
            return item.getQuantity().equals(3) &&
                   item.getTotalPrice().compareTo(new BigDecimal("149.97")) == 0; // 49.99 * 3
        }));
    }

    @Test
    void createOrder_ShouldSetOrderItemOrderReference() {
        // Given
        when(userServiceClient.validateUser(123L)).thenReturn(true);
        when(productServiceClient.getProduct(1L)).thenReturn(testProduct1);
        when(productServiceClient.getProduct(2L)).thenReturn(testProduct2);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        doNothing().when(notificationServiceClient).sendOrderConfirmation(anyLong(), anyLong());

        // When
        orderService.createOrder(testOrderRequest);

        // Then
        verify(orderRepository).save(argThat(order -> {
            return order.getOrderItems().stream()
                   .allMatch(item -> item.getOrder() == order);
        }));
    }
}