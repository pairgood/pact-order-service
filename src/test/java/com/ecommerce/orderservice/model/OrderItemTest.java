package com.ecommerce.orderservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderItemTest {

    private Validator validator;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setProductId(101L);
        orderItem.setProductName("Test Product");
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("49.99"));
        orderItem.setTotalPrice(new BigDecimal("99.98"));
    }

    @Test
    void validOrderItem_ShouldPassValidation() {
        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void orderItemWithNullProductId_ShouldFailValidation() {
        // Given
        orderItem.setProductId(null);

        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("productId");
    }

    @Test
    void orderItemWithNullQuantity_ShouldFailValidation() {
        // Given
        orderItem.setQuantity(null);

        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("quantity");
    }

    @Test
    void orderItemWithNullUnitPrice_ShouldFailValidation() {
        // Given
        orderItem.setUnitPrice(null);

        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("unitPrice");
    }

    @Test
    void orderItemWithNullTotalPrice_ShouldFailValidation() {
        // Given
        orderItem.setTotalPrice(null);

        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("totalPrice");
    }

    @Test
    void gettersAndSetters_ShouldWorkCorrectly() {
        // Test all getters return expected values
        assertThat(orderItem.getId()).isEqualTo(1L);
        assertThat(orderItem.getProductId()).isEqualTo(101L);
        assertThat(orderItem.getProductName()).isEqualTo("Test Product");
        assertThat(orderItem.getQuantity()).isEqualTo(2);
        assertThat(orderItem.getUnitPrice()).isEqualTo(new BigDecimal("49.99"));
        assertThat(orderItem.getTotalPrice()).isEqualTo(new BigDecimal("99.98"));
    }

    @Test
    void setters_ShouldUpdateValues() {
        // Given
        OrderItem newOrderItem = new OrderItem();

        // When
        newOrderItem.setId(2L);
        newOrderItem.setProductId(202L);
        newOrderItem.setProductName("New Product");
        newOrderItem.setQuantity(3);
        newOrderItem.setUnitPrice(new BigDecimal("29.99"));
        newOrderItem.setTotalPrice(new BigDecimal("89.97"));

        // Then
        assertThat(newOrderItem.getId()).isEqualTo(2L);
        assertThat(newOrderItem.getProductId()).isEqualTo(202L);
        assertThat(newOrderItem.getProductName()).isEqualTo("New Product");
        assertThat(newOrderItem.getQuantity()).isEqualTo(3);
        assertThat(newOrderItem.getUnitPrice()).isEqualTo(new BigDecimal("29.99"));
        assertThat(newOrderItem.getTotalPrice()).isEqualTo(new BigDecimal("89.97"));
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyOrderItem() {
        // When
        OrderItem newOrderItem = new OrderItem();

        // Then
        assertThat(newOrderItem.getId()).isNull();
        assertThat(newOrderItem.getOrder()).isNull();
        assertThat(newOrderItem.getProductId()).isNull();
        assertThat(newOrderItem.getProductName()).isNull();
        assertThat(newOrderItem.getQuantity()).isNull();
        assertThat(newOrderItem.getUnitPrice()).isNull();
        assertThat(newOrderItem.getTotalPrice()).isNull();
    }

    @Test
    void parameterizedConstructor_ShouldSetCorrectValues() {
        // Given
        Long productId = 303L;
        String productName = "Constructor Product";
        Integer quantity = 4;
        BigDecimal unitPrice = new BigDecimal("19.99");

        // When
        OrderItem newOrderItem = new OrderItem(productId, productName, quantity, unitPrice);

        // Then
        assertThat(newOrderItem.getProductId()).isEqualTo(productId);
        assertThat(newOrderItem.getProductName()).isEqualTo(productName);
        assertThat(newOrderItem.getQuantity()).isEqualTo(quantity);
        assertThat(newOrderItem.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(newOrderItem.getTotalPrice()).isEqualTo(new BigDecimal("79.96"));
    }

    @Test
    void parameterizedConstructor_ShouldCalculateTotalPrice() {
        // Given
        BigDecimal unitPrice = new BigDecimal("25.50");
        Integer quantity = 3;

        // When
        OrderItem newOrderItem = new OrderItem(123L, "Test Product", quantity, unitPrice);

        // Then
        BigDecimal expectedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        assertThat(newOrderItem.getTotalPrice()).isEqualTo(expectedTotal);
        assertThat(newOrderItem.getTotalPrice()).isEqualTo(new BigDecimal("76.50"));
    }

    @Test
    void orderRelationship_ShouldBeManageable() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUserId(123L);

        // When
        orderItem.setOrder(order);

        // Then
        assertThat(orderItem.getOrder()).isNotNull();
        assertThat(orderItem.getOrder().getId()).isEqualTo(1L);
        assertThat(orderItem.getOrder().getUserId()).isEqualTo(123L);
    }

    @Test
    void orderItemWithZeroQuantity_ShouldBeValid() {
        // Given
        orderItem.setQuantity(0);
        orderItem.setTotalPrice(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void orderItemWithZeroPrice_ShouldBeValid() {
        // Given
        orderItem.setUnitPrice(BigDecimal.ZERO);
        orderItem.setTotalPrice(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void productNameCanBeNull_ShouldBeValid() {
        // Given
        orderItem.setProductName(null);

        // When
        Set<ConstraintViolation<OrderItem>> violations = validator.validate(orderItem);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void totalPriceCalculation_ShouldBeAccurate() {
        // Given
        BigDecimal unitPrice = new BigDecimal("12.345");
        Integer quantity = 7;

        // When
        OrderItem item = new OrderItem(999L, "Precise Product", quantity, unitPrice);

        // Then
        BigDecimal expectedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        assertThat(item.getTotalPrice()).isEqualTo(expectedTotal);
        assertThat(item.getTotalPrice()).isEqualTo(new BigDecimal("86.415"));
    }
}