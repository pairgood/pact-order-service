package com.ecommerce.orderservice.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive Playwright User Acceptance Tests for Order Management System
 * 
 * These tests follow the "As a [user], I want [goal], so that [benefit]" format
 * for clear user story-driven testing scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderManagementPlaywrightTest {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;
    private String baseUrl;

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Boolean.parseBoolean(System.getProperty("playwright.headless", "true")))
                .setSlowMo(Integer.parseInt(System.getProperty("playwright.slowMo", "0"))));
    }

    @AfterAll
    static void afterAll() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("As a user, I want to access the order management system, so that I can manage orders")
    void shouldLoadOrderManagementHomePage() {
        // Given: I am a user wanting to access the order management system
        
        // When: I navigate to the application URL
        page.navigate(baseUrl);
        
        // Then: I should see the Order Management System page
        assertThat(page).hasTitle("Order Management System");
        assertThat(page.locator("h1")).hasText("Order Management System");
        
        // And: I should see the main navigation options
        assertThat(page.locator("#view-orders-btn")).isVisible();
        assertThat(page.locator("#create-order-btn")).isVisible();
        
        // And: The view orders section should be active by default
        assertThat(page.locator("#view-orders-btn")).hasClass("nav-btn active");
        assertThat(page.locator("#orders-section")).hasClass("section active");
    }

    @Test
    @Order(2)
    @DisplayName("As a user, I want to view all existing orders, so that I can see the current order status")
    void shouldDisplayExistingOrders() {
        // Given: I am on the order management system
        page.navigate(baseUrl);
        
        // When: I view the orders list (default view)
        page.waitForSelector("#orders-list");
        
        // Then: I should see existing orders displayed
        assertThat(page.locator(".order-card")).not().hasCount(0);
        
        // And: Each order should display essential information
        Locator firstOrder = page.locator(".order-card").first();
        assertThat(firstOrder.locator(".order-id")).isVisible();
        assertThat(firstOrder.locator(".order-status")).isVisible();
        assertThat(firstOrder.locator(".order-info")).not().hasCount(0);
        
        // And: Orders should have different statuses visible
        assertTrue(page.locator(".order-status").count() > 0, "Should display order statuses");
    }

    @Test
    @Order(3)
    @DisplayName("As a user, I want to filter orders by user ID, so that I can find specific customer orders")
    void shouldFilterOrdersByUserId() {
        // Given: I am viewing the orders list
        page.navigate(baseUrl);
        page.waitForSelector("#orders-list");
        
        // When: I enter a user ID in the filter and apply it
        page.fill("#user-filter", "1");
        page.click("#apply-filters-btn");
        
        // Then: Only orders for that user should be displayed
        page.waitForTimeout(500); // Allow filtering to complete
        Locator orderCards = page.locator(".order-card");
        
        // Verify that all visible orders contain the filtered user ID
        for (int i = 0; i < orderCards.count(); i++) {
            Locator userIdElement = orderCards.nth(i).locator(".order-info").filter(
                new Locator.FilterOptions().setHasText("User ID:")
            ).locator("span");
            assertThat(userIdElement).hasText("1");
        }
    }

    @Test
    @Order(4)
    @DisplayName("As a user, I want to filter orders by status, so that I can focus on orders in specific states")
    void shouldFilterOrdersByStatus() {
        // Given: I am viewing the orders list
        page.navigate(baseUrl);
        page.waitForSelector("#orders-list");
        
        // When: I select a status filter and apply it
        page.selectOption("#status-filter", "DELIVERED");
        page.click("#apply-filters-btn");
        
        // Then: Only orders with that status should be displayed
        page.waitForTimeout(500); // Allow filtering to complete
        Locator orderCards = page.locator(".order-card");
        
        // Verify that all visible orders have the filtered status
        for (int i = 0; i < orderCards.count(); i++) {
            assertThat(orderCards.nth(i).locator(".order-status")).hasText("DELIVERED");
        }
    }

    @Test
    @Order(5)
    @DisplayName("As a user, I want to view detailed information about an order, so that I can see complete order details")
    void shouldDisplayOrderDetailsInModal() {
        // Given: I am viewing the orders list
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        
        // When: I click on an order to view its details
        page.locator(".order-card").first().click();
        
        // Then: A modal should open with detailed order information
        assertThat(page.locator("#order-modal")).hasClass("modal show");
        assertThat(page.locator(".modal-header h3")).hasText("Order Details");
        
        // And: The modal should contain comprehensive order information
        assertThat(page.locator("#order-details .order-info")).not().hasCount(0);
        assertThat(page.locator("#order-details")).containsText("Order ID:");
        assertThat(page.locator("#order-details")).containsText("User ID:");
        assertThat(page.locator("#order-details")).containsText("Status:");
        assertThat(page.locator("#order-details")).containsText("Total Amount:");
        assertThat(page.locator("#order-details")).containsText("Order Date:");
        assertThat(page.locator("#order-details")).containsText("Shipping Address:");
        assertThat(page.locator("#order-details")).containsText("Order Items:");
    }

    @Test
    @Order(6)
    @DisplayName("As a user, I want to close the order details modal, so that I can return to the orders list")
    void shouldCloseOrderDetailsModal() {
        // Given: I have an order details modal open
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        page.locator(".order-card").first().click();
        assertThat(page.locator("#order-modal")).hasClass("modal show");
        
        // When: I click the close button
        page.click(".modal-close");
        
        // Then: The modal should close
        assertThat(page.locator("#order-modal")).not().hasClass("modal show");
        
        // Alternative: Close by clicking outside the modal
        page.locator(".order-card").first().click();
        assertThat(page.locator("#order-modal")).hasClass("modal show");
        
        // When: I click outside the modal content
        page.locator("#order-modal").click();
        
        // Then: The modal should close
        assertThat(page.locator("#order-modal")).not().hasClass("modal show");
    }

    @Test
    @Order(7)
    @DisplayName("As a user, I want to navigate to the create order form, so that I can add new orders")
    void shouldNavigateToCreateOrderForm() {
        // Given: I am on the order management system
        page.navigate(baseUrl);
        
        // When: I click the Create Order navigation button
        page.click("#create-order-btn");
        
        // Then: The create order section should become active
        assertThat(page.locator("#create-order-btn")).hasClass("nav-btn active");
        assertThat(page.locator("#create-section")).hasClass("section active");
        assertThat(page.locator("#orders-section")).not().hasClass("section active");
        
        // And: The create order form should be visible
        assertThat(page.locator("#create-order-form")).isVisible();
        assertThat(page.locator("h2")).hasText("Create New Order");
    }

    @Test
    @Order(8)
    @DisplayName("As a user, I want to see all required fields in the create order form, so that I know what information is needed")
    void shouldDisplayCreateOrderFormFields() {
        // Given: I am on the create order page
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        // When: I view the create order form
        
        // Then: All required form fields should be visible
        assertThat(page.locator("#user-id")).isVisible();
        assertThat(page.locator("#shipping-address")).isVisible();
        
        // And: Order items section should be present
        assertThat(page.locator("#order-items")).isVisible();
        assertThat(page.locator(".order-item")).not().hasCount(0);
        
        // And: Item fields should be present
        Locator firstItem = page.locator(".order-item").first();
        assertThat(firstItem.locator("input[name='productId']")).isVisible();
        assertThat(firstItem.locator("input[name='productName']")).isVisible();
        assertThat(firstItem.locator("input[name='quantity']")).isVisible();
        assertThat(firstItem.locator("input[name='unitPrice']")).isVisible();
        
        // And: Form action buttons should be present
        assertThat(page.locator("button[type='submit']")).isVisible();
        assertThat(page.locator("button[type='reset']")).isVisible();
    }

    @Test
    @Order(9)
    @DisplayName("As a user, I want to add multiple items to an order, so that I can create orders with several products")
    void shouldAllowAddingMultipleOrderItems() {
        // Given: I am on the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        // When: I click the Add Item button
        int initialItemCount = page.locator(".order-item").count();
        page.click("#add-item-btn");
        
        // Then: A new order item row should be added
        assertThat(page.locator(".order-item")).hasCount(initialItemCount + 1);
        
        // When: I click Add Item again
        page.click("#add-item-btn");
        
        // Then: Another order item row should be added
        assertThat(page.locator(".order-item")).hasCount(initialItemCount + 2);
    }

    @Test
    @Order(10)
    @DisplayName("As a user, I want to remove items from an order, so that I can correct mistakes or change the order")
    void shouldAllowRemovingOrderItems() {
        // Given: I have multiple items in the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        page.click("#add-item-btn"); // Add a second item
        page.click("#add-item-btn"); // Add a third item
        
        int initialItemCount = page.locator(".order-item").count();
        assertTrue(initialItemCount >= 2, "Should have at least 2 items");
        
        // When: I click the Remove button on one of the items
        page.locator(".btn-remove").first().click();
        
        // Then: The item should be removed
        assertThat(page.locator(".order-item")).hasCount(initialItemCount - 1);
    }

    @Test
    @Order(11)
    @DisplayName("As a user, I want to create a new order with valid data, so that I can add orders to the system")
    void shouldCreateNewOrderSuccessfully() {
        // Given: I am on the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        // When: I fill in all required fields with valid data
        page.fill("#user-id", "999");
        page.fill("#shipping-address", "123 Test Street, Test City, TC 12345");
        
        // Fill in order item details
        Locator firstItem = page.locator(".order-item").first();
        firstItem.locator("input[name='productId']").fill("1001");
        firstItem.locator("input[name='productName']").fill("Test Product");
        firstItem.locator("input[name='quantity']").fill("2");
        firstItem.locator("input[name='unitPrice']").fill("25.99");
        
        // And: I submit the form
        page.click("button[type='submit']");
        
        // Then: A success message should appear
        page.waitForSelector(".success", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertThat(page.locator(".success")).isVisible();
        assertThat(page.locator(".success")).containsText("Order created successfully");
        
        // And: The form should be reset
        assertThat(page.locator("#user-id")).hasValue("");
        assertThat(page.locator("#shipping-address")).hasValue("");
    }

    @Test
    @Order(12)
    @DisplayName("As a user, I want to see validation errors for incomplete forms, so that I know what information is missing")
    void shouldShowValidationForIncompleteForm() {
        // Given: I am on the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        // When: I try to submit the form without filling required fields
        page.click("button[type='submit']");
        
        // Then: The browser should prevent submission and show validation
        // Note: HTML5 validation will prevent form submission
        assertThat(page.locator("#user-id")).hasAttribute("required", "");
        assertThat(page.locator("#shipping-address")).hasAttribute("required", "");
        assertThat(page.locator("input[name='productId']")).hasAttribute("required", "");
    }

    @Test
    @Order(13)
    @DisplayName("As a user, I want to reset the create order form, so that I can start over with a clean form")
    void shouldResetCreateOrderForm() {
        // Given: I have partially filled the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        page.fill("#user-id", "123");
        page.fill("#shipping-address", "Some address");
        page.locator("input[name='productId']").first().fill("456");
        page.locator("input[name='productName']").first().fill("Some product");
        
        // When: I click the Reset button
        page.click("button[type='reset']");
        
        // Then: All form fields should be cleared
        assertThat(page.locator("#user-id")).hasValue("");
        assertThat(page.locator("#shipping-address")).hasValue("");
        assertThat(page.locator("input[name='productId']").first()).hasValue("");
        assertThat(page.locator("input[name='productName']").first()).hasValue("");
    }

    @Test
    @Order(14)
    @DisplayName("As a user, I want to navigate back to the orders list, so that I can view orders after creating one")
    void shouldNavigateBackToOrdersList() {
        // Given: I am on the create order page
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        assertThat(page.locator("#create-section")).hasClass("section active");
        
        // When: I click the View Orders navigation button
        page.click("#view-orders-btn");
        
        // Then: The orders section should become active
        assertThat(page.locator("#view-orders-btn")).hasClass("nav-btn active");
        assertThat(page.locator("#orders-section")).hasClass("section active");
        assertThat(page.locator("#create-section")).not().hasClass("section active");
        
        // And: The orders list should be refreshed and visible
        page.waitForSelector("#orders-list");
        assertThat(page.locator(".order-card")).not().hasCount(0);
    }

    @Test
    @Order(15)
    @DisplayName("As a user, I want to see responsive design elements, so that I can use the system on different devices")
    void shouldDisplayResponsiveDesignElements() {
        // Given: I am on the order management system
        page.navigate(baseUrl);
        
        // When: I view the page in different viewport sizes
        
        // Desktop view
        page.setViewportSize(1200, 800);
        assertThat(page.locator(".container")).isVisible();
        assertThat(page.locator("header")).isVisible();
        assertThat(page.locator("nav")).isVisible();
        
        // Tablet view
        page.setViewportSize(768, 1024);
        assertThat(page.locator(".container")).isVisible();
        assertThat(page.locator("header")).isVisible();
        
        // Mobile view
        page.setViewportSize(375, 667);
        assertThat(page.locator(".container")).isVisible();
        assertThat(page.locator("header")).isVisible();
        
        // Then: The layout should remain functional across all viewport sizes
        assertThat(page.locator("h1")).isVisible();
        assertThat(page.locator("#view-orders-btn")).isVisible();
        assertThat(page.locator("#create-order-btn")).isVisible();
    }

    @Test
    @Order(16)
    @DisplayName("As a user, I want to see proper loading states, so that I know the system is processing my requests")
    void shouldDisplayLoadingStates() {
        // Given: I am on the order management system
        page.navigate(baseUrl);
        
        // When: The page loads
        // Then: Loading states should be handled gracefully
        // Note: This test verifies that the page loads without showing permanent loading indicators
        
        page.waitForSelector("#orders-list");
        
        // Verify that we don't see persistent loading messages
        Locator loadingElements = page.locator(".loading");
        if (loadingElements.count() > 0) {
            // If loading elements exist, they should not contain "Loading..." text indefinitely
            page.waitForTimeout(2000);
            // After a reasonable time, loading should be complete
            assertThat(page.locator(".order-card, .loading")).not().hasCount(0);
        }
    }
}