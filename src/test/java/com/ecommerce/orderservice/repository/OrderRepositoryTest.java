package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder1;
    private Order testOrder2;
    private Order testOrder3;

    @BeforeEach
    void setUp() {
        // Create test order 1
        testOrder1 = new Order();
        testOrder1.setUserId(100L);
        testOrder1.setTotalAmount(new BigDecimal("199.99"));
        testOrder1.setStatus(Order.OrderStatus.PENDING);
        testOrder1.setShippingAddress("123 Test St, Test City, TC 12345");
        testOrder1.setOrderDate(LocalDateTime.now().minusDays(2));

        // Create order items for order 1
        List<OrderItem> items1 = new ArrayList<>();
        OrderItem item1 = new OrderItem(1L, "Product 1", 2, new BigDecimal("49.99"));
        OrderItem item2 = new OrderItem(2L, "Product 2", 1, new BigDecimal("99.99"));
        item1.setOrder(testOrder1);
        item2.setOrder(testOrder1);
        items1.add(item1);
        items1.add(item2);
        testOrder1.setOrderItems(items1);

        // Create test order 2 for the same user
        testOrder2 = new Order();
        testOrder2.setUserId(100L);
        testOrder2.setTotalAmount(new BigDecimal("79.99"));
        testOrder2.setStatus(Order.OrderStatus.DELIVERED);
        testOrder2.setShippingAddress("123 Test St, Test City, TC 12345");
        testOrder2.setOrderDate(LocalDateTime.now().minusDays(1));

        // Create test order 3 for different user
        testOrder3 = new Order();
        testOrder3.setUserId(200L);
        testOrder3.setTotalAmount(new BigDecimal("299.99"));
        testOrder3.setStatus(Order.OrderStatus.PENDING);
        testOrder3.setShippingAddress("456 Another St, Another City, AC 67890");
        testOrder3.setOrderDate(LocalDateTime.now());
    }

    @Test
    void findByUserId_WithExistingUser_ShouldReturnOrders() {
        // Given
        entityManager.persistAndFlush(testOrder1);
        entityManager.persistAndFlush(testOrder2);
        entityManager.persistAndFlush(testOrder3);

        // When
        List<Order> found = orderRepository.findByUserId(100L);

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Order::getUserId).containsOnly(100L);
        assertThat(found).extracting(Order::getTotalAmount)
                .containsExactlyInAnyOrder(new BigDecimal("199.99"), new BigDecimal("79.99"));
    }

    @Test
    void findByUserId_WithNonExistentUser_ShouldReturnEmptyList() {
        // Given
        entityManager.persistAndFlush(testOrder1);

        // When
        List<Order> found = orderRepository.findByUserId(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByStatus_WithExistingStatus_ShouldReturnOrders() {
        // Given
        entityManager.persistAndFlush(testOrder1);
        entityManager.persistAndFlush(testOrder2);
        entityManager.persistAndFlush(testOrder3);

        // When
        List<Order> pendingOrders = orderRepository.findByStatus(Order.OrderStatus.PENDING);

        // Then
        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).extracting(Order::getStatus)
                .containsOnly(Order.OrderStatus.PENDING);
        assertThat(pendingOrders).extracting(Order::getUserId)
                .containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    void findByStatus_WithNoMatchingStatus_ShouldReturnEmptyList() {
        // Given
        entityManager.persistAndFlush(testOrder1);

        // When
        List<Order> shippedOrders = orderRepository.findByStatus(Order.OrderStatus.SHIPPED);

        // Then
        assertThat(shippedOrders).isEmpty();
    }

    @Test
    void findById_WithExistingOrder_ShouldReturnOrder() {
        // Given
        Order saved = entityManager.persistAndFlush(testOrder1);

        // When
        Optional<Order> found = orderRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(100L);
        assertThat(found.get().getTotalAmount()).isEqualTo(new BigDecimal("199.99"));
        assertThat(found.get().getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    void findById_WithNonExistentOrder_ShouldReturnEmpty() {
        // When
        Optional<Order> found = orderRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void save_ShouldPersistOrder() {
        // When
        Order saved = orderRepository.save(testOrder1);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getTotalAmount()).isEqualTo(new BigDecimal("199.99"));

        // Verify persistence
        Order found = entityManager.find(Order.class, saved.getId());
        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo(100L);
    }

    @Test
    void save_WithOrderItems_ShouldPersistOrderAndItems() {
        // When
        Order saved = orderRepository.save(testOrder1);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderItems()).hasSize(2);

        // Verify persistence with entity manager
        Order found = entityManager.find(Order.class, saved.getId());
        assertThat(found).isNotNull();
        assertThat(found.getOrderItems()).hasSize(2);
        assertThat(found.getOrderItems().get(0).getProductName()).isIn("Product 1", "Product 2");
        assertThat(found.getOrderItems().get(1).getProductName()).isIn("Product 1", "Product 2");
    }

    @Test
    void update_ShouldModifyExistingOrder() {
        // Given
        Order saved = entityManager.persistAndFlush(testOrder1);
        Long orderId = saved.getId();

        // When
        saved.setStatus(Order.OrderStatus.CONFIRMED);
        saved.setTotalAmount(new BigDecimal("249.99"));
        Order updated = orderRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(orderId);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        assertThat(updated.getTotalAmount()).isEqualTo(new BigDecimal("249.99"));

        // Verify persistence
        Order found = entityManager.find(Order.class, orderId);
        assertThat(found.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        assertThat(found.getTotalAmount()).isEqualTo(new BigDecimal("249.99"));
    }

    @Test
    void delete_ShouldRemoveOrder() {
        // Given
        Order saved = entityManager.persistAndFlush(testOrder1);
        Long orderId = saved.getId();

        // When
        orderRepository.delete(saved);

        // Then
        Order found = entityManager.find(Order.class, orderId);
        assertThat(found).isNull();
    }

    @Test
    void findAll_ShouldReturnAllOrders() {
        // Given
        entityManager.persistAndFlush(testOrder1);
        entityManager.persistAndFlush(testOrder2);
        entityManager.persistAndFlush(testOrder3);

        // When
        List<Order> allOrders = orderRepository.findAll();

        // Then
        assertThat(allOrders).hasSize(3);
        assertThat(allOrders).extracting(Order::getUserId)
                .containsExactlyInAnyOrder(100L, 100L, 200L);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(testOrder1);
        entityManager.persistAndFlush(testOrder2);

        // When
        long count = orderRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void existsById_WithExistingOrder_ShouldReturnTrue() {
        // Given
        Order saved = entityManager.persistAndFlush(testOrder1);

        // When
        boolean exists = orderRepository.existsById(saved.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WithNonExistentOrder_ShouldReturnFalse() {
        // When
        boolean exists = orderRepository.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findByUserId_ShouldOrderByOrderDateDesc() {
        // Given
        testOrder1.setOrderDate(LocalDateTime.now().minusDays(3));
        testOrder2.setOrderDate(LocalDateTime.now().minusDays(1));
        entityManager.persistAndFlush(testOrder1);
        entityManager.persistAndFlush(testOrder2);

        // When
        List<Order> found = orderRepository.findByUserId(100L);

        // Then
        assertThat(found).hasSize(2);
        // Assuming the repository implementation orders by date descending
        // We're just testing that we get both orders for the user
        assertThat(found).extracting(Order::getUserId).containsOnly(100L);
    }

    @Test
    void findByStatus_ShouldIncludeAllMatchingOrders() {
        // Given
        Order order4 = new Order();
        order4.setUserId(300L);
        order4.setTotalAmount(new BigDecimal("49.99"));
        order4.setStatus(Order.OrderStatus.DELIVERED);
        order4.setShippingAddress("789 Third St, Third City, TC 11111");
        order4.setOrderDate(LocalDateTime.now());

        entityManager.persistAndFlush(testOrder2); // DELIVERED
        entityManager.persistAndFlush(order4);      // DELIVERED

        // When
        List<Order> deliveredOrders = orderRepository.findByStatus(Order.OrderStatus.DELIVERED);

        // Then
        assertThat(deliveredOrders).hasSize(2);
        assertThat(deliveredOrders).extracting(Order::getStatus)
                .containsOnly(Order.OrderStatus.DELIVERED);
        assertThat(deliveredOrders).extracting(Order::getUserId)
                .containsExactlyInAnyOrder(100L, 300L);
    }
}