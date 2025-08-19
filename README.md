# Order Service

> **🟡 This service is highlighted in the architecture diagram below**

Order management and processing service for the e-commerce microservices ecosystem.

## Service Role: Both Consumer & Producer
This service consumes User and Product services, and produces data/events for Payment and Notification services.

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐
│   User Service  │    │ Product Service │
│   (Port 8081)   │    │   (Port 8082)   │
│                 │    │                 │
│ • Authentication│    │ • Product Catalog│
│ • User Profiles │    │ • Inventory Mgmt│
│ • JWT Tokens    │    │ • Pricing       │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          │ validates users      │ fetches products
          │                      │
          ▼                      ▼
    ┌─────────────────────────────────┐
    │    🟡  Order Service            │
    │        (Port 8083)              │
    │                                 │
    │ • Order Management              │
    │ • Order Processing              │
    │ • Consumes User & Product APIs  │
    └─────────────┬───────────────────┘
                  │
                  │ triggers payment
                  │
                  ▼
    ┌─────────────────────────────────┐
    │       Payment Service           │
    │       (Port 8084)               │
    │                                 │
    │ • Payment Processing            │
    │ • Gateway Integration           │
    │ • Refund Management             │
    └─────────────┬───────────────────┘
                  │
                  │ sends notifications
                  │
                  ▼
    ┌─────────────────────────────────┐
    │    Notification Service         │
    │       (Port 8085)               │
    │                                 │
    │ • Email Notifications           │
    │ • SMS Notifications             │
    │ • Order & Payment Updates       │
    └─────────────────────────────────┘
                  │ All services send telemetry data
                  │
                  ▼
    ┌─────────────────────────────────┐
    │📊  Telemetry Service            │
    │       (Port 8086)               │
    │                                 │
    │ • Distributed Tracing           │
    │ • Service Metrics               │
    │ • Request Tracking              │
    │ • Performance Monitoring        │
    └─────────────────────────────────┘
```

## Features

- **Order Creation**: Process new orders with user and product validation
- **Order Management**: Complete order lifecycle management
- **Status Tracking**: Real-time order status updates
- **Service Integration**: Seamless integration with User, Product, and Notification services
- **Order History**: Complete order history per user
- **Order Items**: Support for multiple products per order

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **HTTP Client**: Spring WebFlux WebClient
- **Database**: H2 (in-memory)
- **ORM**: Spring Data JPA
- **Java Version**: 17

## API Endpoints

### Order Management
- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/user/{userId}` - Get orders by user ID
- `GET /api/orders` - Get all orders
- `PUT /api/orders/{id}/status` - Update order status
- `DELETE /api/orders/{id}` - Cancel order

## Telemetry Integration

The Order Service sends detailed telemetry data to the Telemetry Service for comprehensive monitoring and observability:

### Telemetry Features
- **Request Tracing**: End-to-end tracing of order processing workflows
- **Service Metrics**: Performance metrics including response times and order processing rates
- **Error Tracking**: Automatic detection and reporting of order processing errors
- **Inter-Service Monitoring**: Tracking of calls to User and Product services
- **Business Metrics**: Order-specific metrics like order volume, success rates, and processing times

### Traced Operations
- Order creation and validation workflows
- User service integration calls
- Product service integration calls
- Order status updates and lifecycle management
- Database operations (order and order item persistence)
- Service health checks and dependency validation

### Telemetry Configuration
The service is configured to send telemetry data to the Telemetry Service:
```yaml
telemetry:
  service:
    url: http://localhost:8086
    enabled: true
  tracing:
    sample-rate: 1.0
  metrics:
    enabled: true
    export-interval: 30s
  dependencies:
    track-user-service: true
    track-product-service: true
```

## Running the Service

### Prerequisites
- Java 17+
- Gradle (or use included Gradle wrapper)
- **User Service** must be running on port 8081
- **Product Service** must be running on port 8082
- **Notification Service** should be running on port 8085

### Start the Service
```bash
./gradlew bootRun
```

The service will start on **port 8083**.

### Database Access
- **H2 Console**: http://localhost:8083/h2-console
- **JDBC URL**: `jdbc:h2:mem:orderdb`
- **Username**: `sa`
- **Password**: (empty)

## Service Dependencies

### Services This Service Consumes
- **User Service (port 8081)**: Validates user existence during order creation
- **Product Service (port 8082)**: Fetches product details, pricing, and availability

### Services That Consume This Service
- **Payment Service**: Uses order data for payment processing
- **Notification Service**: Receives order events for customer notifications

## Data Models

### Order Entity
```json
{
  "id": 1,
  "userId": 1,
  "totalAmount": 1299.99,
  "status": "PENDING",
  "orderDate": "2024-01-15T10:30:00",
  "shippingAddress": "123 Main St, City, State",
  "orderItems": [...]
}
```

### Order Item Entity
```json
{
  "id": 1,
  "productId": 1,
  "productName": "Gaming Laptop",
  "quantity": 1,
  "unitPrice": 1299.99,
  "totalPrice": 1299.99
}
```

### Order Status Values
- `PENDING` - Order created, awaiting confirmation
- `CONFIRMED` - Order confirmed
- `PROCESSING` - Order being processed
- `SHIPPED` - Order shipped
- `DELIVERED` - Order delivered
- `CANCELLED` - Order cancelled

## Example Usage

### Create an Order
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "shippingAddress": "123 Main St, Anytown, ST 12345",
    "items": [
      {
        "productId": 1,
        "quantity": 2
      },
      {
        "productId": 2,
        "quantity": 1
      }
    ]
  }'
```

### Get Order Details
```bash
curl -X GET http://localhost:8083/api/orders/1
```

### Update Order Status
```bash
curl -X PUT http://localhost:8083/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED"}'
```

### Get User's Orders
```bash
curl -X GET http://localhost:8083/api/orders/user/1
```

## Service Configuration

The service can be configured via `application.yml`:

```yaml
services:
  user-service:
    url: http://localhost:8081
  product-service:
    url: http://localhost:8082
  notification-service:
    url: http://localhost:8085
```

## Error Handling

- **User Not Found**: Returns 400 error if user doesn't exist
- **Product Not Found**: Returns 400 error if product doesn't exist
- **Service Unavailable**: Graceful handling when dependent services are down

## Related Services

- **[User Service](../user-service/README.md)**: Provides user validation
- **[Product Service](../product-service/README.md)**: Provides product details
- **[Payment Service](../payment-service/README.md)**: Consumes order data
- **[Notification Service](../notification-service/README.md)**: Receives order events
- **[Telemetry Service](../telemetry-service/README.md)**: Collects telemetry data from this service