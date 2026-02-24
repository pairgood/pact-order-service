package com.ecommerce.orderservice.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceHealthIndicatorTest {

    @Test
    void shouldReturnDownWhenUserServiceIsUnreachable() {
        UserServiceHealthIndicator indicator = new UserServiceHealthIndicator();
        ReflectionTestUtils.setField(indicator, "userServiceUrl", "http://localhost:1");

        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsKey("responseTimeMs");
    }

    @Test
    void shouldIncludeUrlAndResponseTimeMsInDetails() {
        UserServiceHealthIndicator indicator = new UserServiceHealthIndicator();
        String url = "http://localhost:1";
        ReflectionTestUtils.setField(indicator, "userServiceUrl", url);

        Health health = indicator.health();
        assertThat(health.getDetails()).containsKey("url");
        assertThat(health.getDetails()).containsKey("responseTimeMs");
        assertThat(health.getDetails().get("url")).isEqualTo(url);
    }
}
