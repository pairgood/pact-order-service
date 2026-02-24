package com.ecommerce.orderservice.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceHealthIndicatorTest {

    @Test
    void shouldReturnDetailsWhenHealthCheckRuns() {
        NotificationServiceHealthIndicator indicator = new NotificationServiceHealthIndicator();
        ReflectionTestUtils.setField(indicator, "notificationServiceUrl", "http://localhost:9999");

        Health health = indicator.health();

        assertThat(health).isNotNull();
        assertThat(health.getDetails()).containsKey("url");
        assertThat(health.getDetails()).containsKey("responseTimeMs");
    }

    @Test
    void shouldReturnDownWhenNotificationServiceIsUnreachable() {
        NotificationServiceHealthIndicator indicator = new NotificationServiceHealthIndicator();
        ReflectionTestUtils.setField(indicator, "notificationServiceUrl", "http://localhost:1");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsKey("responseTimeMs");
    }
}
