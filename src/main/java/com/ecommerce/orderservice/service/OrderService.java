package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.telemetry.TelemetryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Autowired
    private NotificationServiceClient notificationServiceClient;
    
    @Autowired
    private TelemetryClient telemetryClient;
    
    private final WebClient webClient = WebClient.builder().build();
    
    public Order createOrder(OrderRequest orderRequest) {
        telemetryClient.logEvent("Validating user: " + orderRequest.getUserId(), "INFO");
        
        // Validate user exists
        boolean userExists = userServiceClient.validateUser(orderRequest.getUserId());
        if (!userExists) {
            telemetryClient.logEvent("User validation failed: " + orderRequest.getUserId(), "ERROR");
            throw new RuntimeException("User not found");
        }
        
        telemetryClient.logEvent("User validated successfully: " + orderRequest.getUserId(), "INFO");
        
        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setShippingAddress(orderRequest.getShippingAddress());
        
        telemetryClient.logEvent("Processing " + orderRequest.getItems().size() + " order items", "INFO");
        
        // Process order items
        List<OrderItem> orderItems = orderRequest.getItems().stream()
            .map(itemRequest -> {
                // Get product details from product service
                ProductDto product = productServiceClient.getProduct(itemRequest.getProductId());
                
                OrderItem orderItem = new OrderItem(
                    product.getId(),
                    product.getName(),
                    itemRequest.getQuantity(),
                    product.getPrice()
                );
                orderItem.setOrder(order);
                return orderItem;
            })
            .collect(Collectors.toList());
        
        order.setOrderItems(orderItems);
        
        // Calculate total amount
        BigDecimal totalAmount = orderItems.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);
        
        Order savedOrder = orderRepository.save(order);
        telemetryClient.logEvent("Order saved to database with ID: " + savedOrder.getId(), "INFO");
        
        // Send notification
        notificationServiceClient.sendOrderConfirmation(savedOrder.getId(), savedOrder.getUserId());
        
        telemetryClient.logEvent("Order created successfully with total amount: " + totalAmount, "INFO");
        
        return savedOrder;
    }
    
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        telemetryClient.logEvent("Updating order status: " + id + " to " + status, "INFO");
        
        Order order = getOrderById(id);
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        telemetryClient.logEvent("Order status updated successfully: " + id, "INFO");
        
        // Send status update notification
        notificationServiceClient.sendOrderStatusUpdate(updatedOrder.getId(), updatedOrder.getUserId(), status.toString());
        
        return updatedOrder;
    }
    
    public void cancelOrder(Long id) {
        telemetryClient.logEvent("Cancelling order: " + id, "INFO");
        
        Order order = getOrderById(id);
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        telemetryClient.logEvent("Order cancelled successfully: " + id, "INFO");
        
        // Send cancellation notification
        notificationServiceClient.sendOrderCancellation(order.getId(), order.getUserId());
    }
    
    public static class ProductDto {
        private Long id;
        private String name;
        private BigDecimal price;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
}