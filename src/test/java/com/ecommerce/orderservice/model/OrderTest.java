package com.ecommerce.orderservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderTest {

    private Validator validator;
    private Order order;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        order = new Order();
        order.setId(1L);
        order.setUserId(123L);
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress("123 Test St, Test City, TC 12345");
        order.setOrderDate(LocalDateTime.now());
    }

    @Test
    void validOrder_ShouldPassValidation() {
        // When
        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void orderWithNullUserId_ShouldFailValidation() {
        // Given
        order.setUserId(null);

        // When
        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userId");
    }

    @Test
    void orderWithNullTotalAmount_ShouldFailValidation() {
        // Given
        order.setTotalAmount(null);

        // When
        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("totalAmount");
    }

    @Test
    void gettersAndSetters_ShouldWorkCorrectly() {
        // Test all getters return expected values
        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getUserId()).isEqualTo(123L);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(order.getShippingAddress()).isEqualTo("123 Test St, Test City, TC 12345");
        assertThat(order.getOrderDate()).isNotNull();
    }

    @Test
    void setters_ShouldUpdateValues() {
        // Given
        Order newOrder = new Order();
        LocalDateTime now = LocalDateTime.now();

        // When
        newOrder.setId(2L);
        newOrder.setUserId(456L);
        newOrder.setTotalAmount(new BigDecimal("199.99"));
        newOrder.setStatus(Order.OrderStatus.CONFIRMED);
        newOrder.setShippingAddress("456 New St, New City, NC 67890");
        newOrder.setOrderDate(now);

        // Then
        assertThat(newOrder.getId()).isEqualTo(2L);
        assertThat(newOrder.getUserId()).isEqualTo(456L);
        assertThat(newOrder.getTotalAmount()).isEqualTo(new BigDecimal("199.99"));
        assertThat(newOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        assertThat(newOrder.getShippingAddress()).isEqualTo("456 New St, New City, NC 67890");
        assertThat(newOrder.getOrderDate()).isEqualTo(now);
    }

    @Test
    void defaultConstructor_ShouldSetDefaultValues() {
        // When
        Order newOrder = new Order();

        // Then
        assertThat(newOrder.getId()).isNull();
        assertThat(newOrder.getUserId()).isNull();
        assertThat(newOrder.getTotalAmount()).isNull();
        assertThat(newOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(newOrder.getOrderDate()).isNotNull();
        assertThat(newOrder.getShippingAddress()).isNull();
        assertThat(newOrder.getOrderItems()).isNull();
    }

    @Test
    void parameterizedConstructor_ShouldSetCorrectValues() {
        // Given
        Long userId = 789L;
        BigDecimal totalAmount = new BigDecimal("299.99");
        String shippingAddress = "789 Constructor St, Constructor City, CC 11111";

        // When
        Order newOrder = new Order(userId, totalAmount, shippingAddress);

        // Then
        assertThat(newOrder.getUserId()).isEqualTo(userId);
        assertThat(newOrder.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(newOrder.getShippingAddress()).isEqualTo(shippingAddress);
        assertThat(newOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(newOrder.getOrderDate()).isNotNull();
    }

    @Test
    void orderItems_ShouldBeManageableAsCollection() {
        // Given
        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem item1 = new OrderItem(1L, "Product 1", 2, new BigDecimal("49.99"));
        OrderItem item2 = new OrderItem(2L, "Product 2", 1, new BigDecimal("99.99"));
        orderItems.add(item1);
        orderItems.add(item2);

        // When
        order.setOrderItems(orderItems);

        // Then
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getOrderItems().get(0).getProductName()).isEqualTo("Product 1");
        assertThat(order.getOrderItems().get(1).getProductName()).isEqualTo("Product 2");
    }

    @Test
    void orderStatus_ShouldHaveAllExpectedValues() {
        // Test all enum values exist
        assertThat(Order.OrderStatus.PENDING).isNotNull();
        assertThat(Order.OrderStatus.CONFIRMED).isNotNull();
        assertThat(Order.OrderStatus.PROCESSING).isNotNull();
        assertThat(Order.OrderStatus.SHIPPED).isNotNull();
        assertThat(Order.OrderStatus.DELIVERED).isNotNull();
        assertThat(Order.OrderStatus.CANCELLED).isNotNull();

        // Test enum values count
        assertThat(Order.OrderStatus.values()).hasSize(6);
    }

    @Test
    void orderStatus_ShouldBeChangeable() {
        // Test status transitions
        order.setStatus(Order.OrderStatus.PENDING);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        order.setStatus(Order.OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);

        order.setStatus(Order.OrderStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);

        order.setStatus(Order.OrderStatus.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);

        order.setStatus(Order.OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);

        order.setStatus(Order.OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    void orderWithZeroAmount_ShouldBeValid() {
        // Given
        order.setTotalAmount(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void orderWithNegativeAmount_ShouldBeValid() {
        // Given - Allowing negative amounts for potential refunds
        order.setTotalAmount(new BigDecimal("-50.00"));

        // When
        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        // Then
        assertThat(violations).isEmpty();
    }
}