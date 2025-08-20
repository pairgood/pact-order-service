package com.ecommerce.orderservice.config;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class DataLoaderTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DataLoader dataLoader;

    @Test
    void run_WithEmptyDatabase_ShouldLoadSeedData() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);

        // When
        dataLoader.run("arg1", "arg2");

        // Then
        verify(orderRepository).count();
        verify(orderRepository, times(6)).save(any(Order.class));
    }

    @Test
    void run_WithExistingData_ShouldNotLoadSeedData() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(5L);

        // When
        dataLoader.run("arg1", "arg2");

        // Then
        verify(orderRepository).count();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void run_ShouldCreateOrdersWithCorrectData() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // When
        dataLoader.run();

        // Then
        verify(orderRepository, times(6)).save(orderCaptor.capture());
        
        List<Order> savedOrders = orderCaptor.getAllValues();
        assertThat(savedOrders).hasSize(6);

        // Verify first order (John Doe's gaming setup)
        Order firstOrder = savedOrders.get(0);
        assertThat(firstOrder.getId()).isEqualTo(1L);
        assertThat(firstOrder.getUserId()).isEqualTo(1L);
        assertThat(firstOrder.getShippingAddress()).isEqualTo("123 Main St, Anytown, ST 12345");
        assertThat(firstOrder.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        assertThat(firstOrder.getOrderItems()).hasSize(3);
        assertThat(firstOrder.getTotalAmount()).isEqualTo(new BigDecimal("1479.97"));

        // Verify order items for first order
        assertThat(firstOrder.getOrderItems().get(0).getProductName()).isEqualTo("Gaming Laptop Pro");
        assertThat(firstOrder.getOrderItems().get(1).getProductName()).isEqualTo("Wireless Gaming Mouse");
        assertThat(firstOrder.getOrderItems().get(2).getProductName()).isEqualTo("Mechanical Keyboard");

        // Verify second order (Jane Smith's book collection)
        Order secondOrder = savedOrders.get(1);
        assertThat(secondOrder.getUserId()).isEqualTo(2L);
        assertThat(secondOrder.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        assertThat(secondOrder.getOrderItems()).hasSize(3);
        assertThat(secondOrder.getTotalAmount()).isEqualTo(new BigDecimal("159.96"));
    }

    @Test
    void run_ShouldCreateOrdersWithDifferentStatuses() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // When
        dataLoader.run();

        // Then
        verify(orderRepository, times(6)).save(orderCaptor.capture());
        
        List<Order> savedOrders = orderCaptor.getAllValues();
        
        // Check that we have orders with different statuses
        assertThat(savedOrders.get(0).getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        assertThat(savedOrders.get(1).getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        assertThat(savedOrders.get(2).getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
        assertThat(savedOrders.get(3).getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);
        assertThat(savedOrders.get(4).getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(savedOrders.get(5).getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    void run_ShouldCreateOrdersWithCorrectTotalAmounts() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // When
        dataLoader.run();

        // Then
        verify(orderRepository, times(6)).save(orderCaptor.capture());
        
        List<Order> savedOrders = orderCaptor.getAllValues();
        
        // Verify that total amounts are calculated correctly
        for (Order order : savedOrders) {
            BigDecimal calculatedTotal = order.getOrderItems().stream()
                .map(item -> item.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(order.getTotalAmount()).isEqualTo(calculatedTotal);
        }
    }

    @Test
    void run_ShouldSetOrderItemReferences() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // When
        dataLoader.run();

        // Then
        verify(orderRepository, times(6)).save(orderCaptor.capture());
        
        List<Order> savedOrders = orderCaptor.getAllValues();
        
        // Verify that all order items have correct order reference
        for (Order order : savedOrders) {
            for (var item : order.getOrderItems()) {
                assertThat(item.getOrder()).isEqualTo(order);
            }
        }
    }

    @Test
    void run_ShouldCreateOrdersForDifferentUsers() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // When
        dataLoader.run();

        // Then
        verify(orderRepository, times(6)).save(orderCaptor.capture());
        
        List<Order> savedOrders = orderCaptor.getAllValues();
        
        // Verify that orders are created for different users (1-6)
        assertThat(savedOrders).extracting(Order::getUserId)
            .containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void run_ShouldCreateOrdersWithValidOrderDates() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // When
        dataLoader.run();

        // Then
        verify(orderRepository, times(6)).save(orderCaptor.capture());
        
        List<Order> savedOrders = orderCaptor.getAllValues();
        
        // Verify that all orders have order dates set
        for (Order order : savedOrders) {
            assertThat(order.getOrderDate()).isNotNull();
        }
        
        // Orders should be in chronological order (oldest first)
        for (int i = 1; i < savedOrders.size(); i++) {
            assertThat(savedOrders.get(i).getOrderDate())
                .isAfterOrEqualTo(savedOrders.get(i-1).getOrderDate());
        }
    }

    @Test
    void run_ShouldCreateOrderItemsWithCorrectPrices() throws Exception {
        // Given
        when(orderRepository.count()).thenReturn(0L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        // When
        dataLoader.run();

        // Then
        verify(orderRepository, times(6)).save(orderCaptor.capture());
        
        List<Order> savedOrders = orderCaptor.getAllValues();
        
        // Verify that order items have correct calculated prices
        for (Order order : savedOrders) {
            for (var item : order.getOrderItems()) {
                BigDecimal expectedTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
                assertThat(item.getTotalPrice()).isEqualTo(expectedTotal);
                
                assertThat(item.getProductId()).isNotNull();
                assertThat(item.getProductName()).isNotNull();
                assertThat(item.getQuantity()).isGreaterThan(0);
                assertThat(item.getUnitPrice()).isGreaterThan(BigDecimal.ZERO);
            }
        }
    }
}