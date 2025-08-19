package com.ecommerce.orderservice.config;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Only load data if the database is empty
        if (orderRepository.count() == 0) {
            loadSeedData();
        }
    }
    
    private void loadSeedData() {
        System.out.println("🌱 Loading Order Service seed data...");
        
        // Create sample orders with realistic scenarios
        List<Order> orders = new ArrayList<>();
        
        // Order 1: John Doe's gaming setup
        orders.add(createOrder(1L, 1L, "123 Main St, Anytown, ST 12345", 
                              Order.OrderStatus.COMPLETED, LocalDateTime.now().minusDays(5),
                              createOrderItems(
                                  new OrderItemData(1L, "Gaming Laptop Pro", 1, new BigDecimal("1299.99")),
                                  new OrderItemData(2L, "Wireless Gaming Mouse", 1, new BigDecimal("49.99")),
                                  new OrderItemData(3L, "Mechanical Keyboard", 1, new BigDecimal("129.99"))
                              )));
        
        // Order 2: Jane Smith's book collection
        orders.add(createOrder(2L, 2L, "456 Oak Ave, Springfield, IL 62701", 
                              Order.OrderStatus.COMPLETED, LocalDateTime.now().minusDays(3),
                              createOrderItems(
                                  new OrderItemData(6L, "Microservices Architecture", 2, new BigDecimal("39.99")),
                                  new OrderItemData(7L, "Spring Boot in Action", 1, new BigDecimal("44.99")),
                                  new OrderItemData(8L, "Clean Code", 1, new BigDecimal("34.99"))
                              )));
        
        // Order 3: Bob Wilson's home office setup
        orders.add(createOrder(3L, 3L, "789 Pine Rd, Austin, TX 78701", 
                              Order.OrderStatus.SHIPPED, LocalDateTime.now().minusDays(2),
                              createOrderItems(
                                  new OrderItemData(9L, "Smart Coffee Maker", 1, new BigDecimal("149.99")),
                                  new OrderItemData(10L, "LED Desk Lamp", 2, new BigDecimal("29.99")),
                                  new OrderItemData(5L, "Wireless Headphones", 1, new BigDecimal("199.99"))
                              )));
        
        // Order 4: Alice Johnson's fitness gear
        orders.add(createOrder(4L, 4L, "321 Elm St, Denver, CO 80201", 
                              Order.OrderStatus.PROCESSING, LocalDateTime.now().minusDays(1),
                              createOrderItems(
                                  new OrderItemData(11L, "Yoga Mat Premium", 1, new BigDecimal("24.99")),
                                  new OrderItemData(12L, "Water Bottle Insulated", 2, new BigDecimal("19.99")),
                                  new OrderItemData(13L, "Cotton T-Shirt", 3, new BigDecimal("14.99"))
                              )));
        
        // Order 5: Charlie Brown's wardrobe update
        orders.add(createOrder(5L, 5L, "654 Maple Dr, Seattle, WA 98101", 
                              Order.OrderStatus.PENDING, LocalDateTime.now().minusHours(6),
                              createOrderItems(
                                  new OrderItemData(14L, "Denim Jeans", 2, new BigDecimal("59.99")),
                                  new OrderItemData(15L, "Hoodie Pullover", 1, new BigDecimal("39.99")),
                                  new OrderItemData(13L, "Cotton T-Shirt", 4, new BigDecimal("14.99"))
                              )));
        
        // Order 6: Diana Clark's tech upgrade
        orders.add(createOrder(6L, 6L, "987 Cedar Ln, Portland, OR 97201", 
                              Order.OrderStatus.CANCELLED, LocalDateTime.now().minusHours(3),
                              createOrderItems(
                                  new OrderItemData(4L, "4K Webcam", 1, new BigDecimal("89.99")),
                                  new OrderItemData(5L, "Wireless Headphones", 1, new BigDecimal("199.99"))
                              )));
        
        for (Order order : orders) {
            orderRepository.save(order);
        }
        
        System.out.println("✅ Created " + orders.size() + " orders with various statuses");
        System.out.println("📋 Statuses: COMPLETED, SHIPPED, PROCESSING, PENDING, CANCELLED");
    }
    
    private Order createOrder(Long id, Long userId, String shippingAddress, 
                             Order.OrderStatus status, LocalDateTime orderDate, List<OrderItem> items) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setShippingAddress(shippingAddress);
        order.setStatus(status);
        order.setOrderDate(orderDate);
        order.setOrderItems(items);
        
        // Calculate total amount
        BigDecimal totalAmount = items.stream()
            .map(item -> item.getTotalPrice())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);
        
        // Set order reference for all items
        for (OrderItem item : items) {
            item.setOrder(order);
        }
        
        return order;
    }
    
    private List<OrderItem> createOrderItems(OrderItemData... itemsData) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemData data : itemsData) {
            OrderItem item = new OrderItem();
            item.setProductId(data.productId);
            item.setProductName(data.productName);
            item.setQuantity(data.quantity);
            item.setUnitPrice(data.unitPrice);
            item.setTotalPrice(data.unitPrice.multiply(BigDecimal.valueOf(data.quantity)));
            items.add(item);
        }
        return items;
    }
    
    private static class OrderItemData {
        Long productId;
        String productName;
        Integer quantity;
        BigDecimal unitPrice;
        
        OrderItemData(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }
}