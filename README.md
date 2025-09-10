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

## Pact Contract Testing

This service uses [Pact](https://pact.io/) for consumer contract testing to ensure reliable communication with external services.

### Consumer Role

This service acts as a consumer for the following external services:
- **User Service**: GET `/api/users/{userId}` (user validation)
- **Product Service**: GET `/api/products/{productId}` (product details)
- **Notification Service**: POST endpoints for order notifications
- **Telemetry Service**: POST `/api/telemetry/events` (telemetry data)

### Running Pact Tests

#### Consumer Tests
```bash
# Run consumer tests and generate contracts
./gradlew pactTest

# Generated contracts will be in build/pacts/
```

#### Publishing Contracts
```bash
# Publish contracts to Pactflow
./gradlew pactPublish
```

### Contract Testing Approach

This implementation follows Pact's **"Be conservative in what you send"** principle:

- Consumer tests define minimal request structures with only required fields
- Request bodies cannot contain fields not defined in the contract
- Tests validate that actual API calls match contract expectations exactly
- Mock servers reject requests with unexpected extra fields

### Contract Files

Consumer contracts are generated in:
- `build/pacts/` - Local contract files  
- Pactflow - Centralized contract storage and management

### Troubleshooting

#### Common Issues

1. **Consumer Test Failures**
   - **Extra fields in request**: Remove any fields from request body that aren't actually needed
   - **Mock server expectation mismatch**: Verify HTTP method, path, headers, and body structure
   - **Content-Type headers**: Ensure request headers match exactly what the service sends
   - **URL path parameters**: Check that path parameters are correctly formatted in the contract

2. **Contract Generation Issues**
   - **Missing @Pact annotation**: Ensure each contract method has proper annotations
   - **Invalid JSON structure**: Verify LambdaDsl body definitions match actual data structures
   - **Provider state setup**: Ensure provider state descriptions are descriptive and specific

3. **Pactflow Integration Issues**
   - **Authentication**: Verify `PACT_BROKER_TOKEN` environment variable is set
   - **Base URL**: Confirm `PACT_BROKER_BASE_URL` points to `https://pairgood.pactflow.io`
   - **Network connectivity**: Check firewall/proxy settings if publishing fails

#### Debug Commands

```bash
# Run with debug output
./gradlew pactTest --info --debug

# Run specific test class
./gradlew pactTest --tests="*UserServicePactTest*"

# Generate contracts without publishing
./gradlew pactTest -x pactPublish

# Clean and regenerate contracts
./gradlew clean pactTest
```

#### Debug Logging

Add to `application-test.properties` for detailed Pact logging:
```properties
logging.level.au.com.dius.pact=DEBUG
logging.level.org.apache.http=DEBUG
```

### Contract Evolution

When external services change their APIs:

1. **New Fields in Responses**: No action needed - consumers ignore extra fields
2. **Removed Response Fields**: Update consumer tests if those fields were being used
3. **New Required Request Fields**: Update consumer tests and service code
4. **Changed Endpoints**: Update consumer contract paths and service client code

### Integration with CI/CD

Consumer contract tests run automatically on:
- **Pull Requests**: Generate and validate contracts
- **Main Branch**: Publish contracts to Pactflow for provider verification
- **Feature Branches**: Generate contracts for validation (not published)

### Manual Testing

For local development against real services:
```bash
# Test against local services (disable Pact)
./gradlew test -Dpact.verifier.disabled=true

# Test against staging services
export EXTERNAL_SERVICE_URL=https://staging.example.com
./gradlew test -Dpact.verifier.disabled=true
```

### Contract Documentation

Generated contracts document:
- **API interactions**: What endpoints this service calls
- **Request formats**: Exact structure of requests sent
- **Response expectations**: What fields this service relies on
- **Error handling**: How this service handles different response scenarios

### Current Status

⚠️ **Note**: Consumer contract tests are currently disabled due to compatibility issues between Pact framework version 4.6.2 and the current Spring Boot 3.2.0 + JUnit 5 setup. The test structure and client modifications have been implemented and are ready for activation once compatibility is resolved.

**Next Steps**:
1. Resolve Pact framework compatibility (investigating versions compatible with Spring Boot 3.x)
2. Enable Pact tests in CI/CD pipeline
3. Publish contracts to Pactflow for provider verification

**Test Files Ready**:
- `UserServicePactTest.java` - User validation contracts
- `ProductServicePactTest.java` - Product fetching contracts  
- `NotificationServicePactTest.java` - Order notification contracts
- `TelemetryServicePactTest.java` - Telemetry event contracts
- **[Telemetry Service](../telemetry-service/README.md)**: Collects telemetry data from this service