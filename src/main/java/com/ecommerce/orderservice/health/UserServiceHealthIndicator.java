package com.ecommerce.orderservice.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceHealthIndicator implements HealthIndicator {

    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(2000);
            factory.setReadTimeout(3000);

            RestTemplate restTemplate = new RestTemplate(factory);
            restTemplate.getForObject(userServiceUrl + "/actuator/health", String.class);

            long responseTime = System.currentTimeMillis() - startTime;
            return Health.up()
                    .withDetail("url", userServiceUrl)
                    .withDetail("responseTimeMs", responseTime)
                    .build();

        } catch (RestClientException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return Health.down()
                    .withDetail("url", userServiceUrl)
                    .withDetail("error", e.getMessage())
                    .withDetail("responseTimeMs", responseTime)
                    .build();
        }
    }
}
