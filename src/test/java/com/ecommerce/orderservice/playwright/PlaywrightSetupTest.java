package com.ecommerce.orderservice.playwright;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic Playwright setup test to verify the testing framework is properly configured
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PlaywrightSetupTest {

    @Test
    @DisplayName("As a developer, I want to verify Playwright testing framework is set up, so that I can run user acceptance tests")
    void shouldVerifyPlaywrightTestFrameworkSetup() {
        // Given: Playwright dependencies are configured in the project
        
        // When: This test runs
        boolean playwrightClassExists = true;
        try {
            Class.forName("com.microsoft.playwright.Playwright");
        } catch (ClassNotFoundException e) {
            playwrightClassExists = false;
        }
        
        // Then: Playwright classes should be available
        assertTrue(playwrightClassExists, "Playwright framework should be available on the classpath");
    }
}