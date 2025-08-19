package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.telemetry.TelemetryClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Order Management", description = "API for managing customer orders including creation, tracking, status updates, and cancellation")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TelemetryClient telemetryClient;
    
    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order for a customer with the specified products and quantities")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid order data provided"),
        @ApiResponse(responseCode = "404", description = "User or product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest orderRequest, HttpServletRequest request) {
        String traceId = telemetryClient.startTrace("create_order", "POST", request.getRequestURL().toString(), orderRequest.getUserId().toString());
        
        try {
            telemetryClient.logEvent("Order creation started for user: " + orderRequest.getUserId(), "INFO");
            Order order = orderService.createOrder(orderRequest);
            telemetryClient.logEvent("Order created successfully with ID: " + order.getId(), "INFO");
            telemetryClient.finishTrace("create_order", 200, null);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            telemetryClient.finishTrace("create_order", 500, e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves a specific order using its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order found and returned successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found with the provided ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Order> getOrderById(
        @Parameter(description = "Unique identifier of the order", required = true, example = "1")
        @PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user ID", description = "Retrieves all orders associated with a specific customer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully for the user"),
        @ApiResponse(responseCode = "404", description = "No orders found for the specified user"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Order>> getOrdersByUserId(
        @Parameter(description = "Unique identifier of the user", required = true, example = "123")
        @PathVariable Long userId) {
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping
    @Operation(summary = "Retrieve all orders", description = "Returns a list of all orders in the system (admin access typically required)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Updates the status of an existing order (e.g., PENDING, CONFIRMED, SHIPPED, DELIVERED)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status provided"),
        @ApiResponse(responseCode = "404", description = "Order not found with the provided ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Order> updateOrderStatus(
        @Parameter(description = "Unique identifier of the order to update", required = true, example = "1")
        @PathVariable Long id, 
        @RequestBody OrderStatusUpdateRequest request) {
        Order order = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(order);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order", description = "Cancels an existing order and processes any necessary refunds")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Order cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Order cannot be cancelled (e.g., already shipped)"),
        @ApiResponse(responseCode = "404", description = "Order not found with the provided ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> cancelOrder(
        @Parameter(description = "Unique identifier of the order to cancel", required = true, example = "1")
        @PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
    
    public static class OrderStatusUpdateRequest {
        private Order.OrderStatus status;
        
        public Order.OrderStatus getStatus() { return status; }
        public void setStatus(Order.OrderStatus status) { this.status = status; }
    }
}