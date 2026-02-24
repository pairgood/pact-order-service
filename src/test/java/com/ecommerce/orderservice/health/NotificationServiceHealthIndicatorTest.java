package com.ecommerce.orderservice.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceHealthIndicatorTest {

    @Test
    void shouldReturnDownWhenNotificationServiceIsUnreachable() {
        NotificationServiceHealthIndicator indicator = new NotificationServiceHealthIndicator();
        ReflectionTestUtils.setField(indicator, "notificationServiceUrl", "http://localhost:1");

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsKey("responseTimeMs");
    }

    @Test
    void shouldIncludeUrlAndResponseTimeMsInDetails() {
        NotificationServiceHealthIndicator indicator = new NotificationServiceHealthIndicator();
        String url = "http://localhost:1";
        ReflectionTestUtils.setField(indicator, "notificationServiceUrl", url);

        Health health = indicator.health();
        assertThat(health.getDetails()).containsKey("url");
        assertThat(health.getDetails()).containsKey("responseTimeMs");
        assertThat(health.getDetails().get("url")).isEqualTo(url);
    }
}
