package com.ecommerce.orderservice.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Checks the health of notification-service by calling its /actuator/health endpoint.
 *
 * Design rules applied here:
 * - 2s connect / 3s read timeout prevents this check from blocking the health endpoint
 * - try/catch returns DOWN with error detail rather than propagating exceptions
 * - responseTimeMs is included so Spring Boot Admin shows more than just UP/DOWN
 * - Calls /actuator/health, not a business endpoint
 * - Reuses existing config key services.notification-service.url
 */
@Component
public class NotificationServiceHealthIndicator implements HealthIndicator {

    @Value("${services.notification-service.url:http://localhost:8085}")
    private String notificationServiceUrl;

    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(2000);
            factory.setReadTimeout(3000);

            RestTemplate restTemplate = new RestTemplate(factory);
            restTemplate.getForObject(notificationServiceUrl + "/actuator/health", String.class);

            long responseTime = System.currentTimeMillis() - startTime;
            return Health.up()
                    .withDetail("url", notificationServiceUrl)
                    .withDetail("responseTimeMs", responseTime)
                    .build();

        } catch (RestClientException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return Health.down()
                    .withDetail("url", notificationServiceUrl)
                    .withDetail("error", e.getMessage())
                    .withDetail("responseTimeMs", responseTime)
                    .build();
        }
    }
}
