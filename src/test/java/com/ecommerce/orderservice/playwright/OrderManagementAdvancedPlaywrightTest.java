package com.ecommerce.orderservice.playwright;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Advanced Playwright User Acceptance Tests for Order Management Operations
 * 
 * These tests focus on order status management and advanced user interactions
 * following the "As a [user], I want [goal], so that [benefit]" format.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderManagementAdvancedPlaywrightTest {

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
    @DisplayName("As an admin, I want to update order status, so that I can track order progress")
    void shouldUpdateOrderStatusSuccessfully() {
        // Given: I have an order details modal open
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        page.locator(".order-card").first().click();
        assertThat(page.locator("#order-modal")).hasClass("modal show");
        
        // When: I select a new status and update it
        page.selectOption("#status-update", "PROCESSING");
        page.click("#update-status-btn");
        
        // Then: A success message should appear
        page.waitForSelector(".success", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertThat(page.locator(".success")).isVisible();
        assertThat(page.locator(".success")).containsText("updated successfully");
        
        // And: The modal should close
        assertThat(page.locator("#order-modal")).not().hasClass("modal show");
    }

    @Test
    @Order(2)
    @DisplayName("As an admin, I want to cancel an order, so that I can handle order cancellation requests")
    void shouldCancelOrderWithConfirmation() {
        // Given: I have an order details modal open
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        page.locator(".order-card").first().click();
        assertThat(page.locator("#order-modal")).hasClass("modal show");
        
        // When: I click the cancel order button
        // Note: We need to handle the confirmation dialog
        page.onDialog(dialog -> {
            assertTrue(dialog.message().contains("Are you sure"), "Should show confirmation dialog");
            dialog.accept();
        });
        
        page.click("#cancel-order-btn");
        
        // Then: A success message should appear
        page.waitForSelector(".success", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertThat(page.locator(".success")).isVisible();
        assertThat(page.locator(".success")).containsText("cancelled successfully");
        
        // And: The modal should close
        assertThat(page.locator("#order-modal")).not().hasClass("modal show");
    }

    @Test
    @Order(3)
    @DisplayName("As a user, I want to decline order cancellation, so that I can keep the order if I change my mind")
    void shouldNotCancelOrderWhenDecliningConfirmation() {
        // Given: I have an order details modal open
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        page.locator(".order-card").first().click();
        assertThat(page.locator("#order-modal")).hasClass("modal show");
        
        // When: I click the cancel order button but decline the confirmation
        page.onDialog(dialog -> {
            assertTrue(dialog.message().contains("Are you sure"), "Should show confirmation dialog");
            dialog.dismiss();
        });
        
        page.click("#cancel-order-btn");
        
        // Then: No success message should appear
        page.waitForTimeout(1000);
        assertThat(page.locator(".success")).not().isVisible();
        
        // And: The modal should remain open
        assertThat(page.locator("#order-modal")).hasClass("modal show");
    }

    @Test
    @Order(4)
    @DisplayName("As a user, I want to see proper order status styling, so that I can quickly identify order states")
    void shouldDisplayCorrectStatusStyling() {
        // Given: I am viewing orders
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        
        // When: I view orders with different statuses
        Locator statusElements = page.locator(".order-status");
        
        // Then: Each status should have appropriate CSS classes
        for (int i = 0; i < statusElements.count(); i++) {
            Locator statusElement = statusElements.nth(i);
            String statusText = statusElement.textContent().toLowerCase();
            String className = statusElement.getAttribute("class");
            
            // Verify that status has appropriate styling class
            assertTrue(className.contains("status-" + statusText), 
                "Status '" + statusText + "' should have class 'status-" + statusText + "'");
        }
    }

    @Test
    @Order(5)
    @DisplayName("As a user, I want to see order items in the details modal, so that I can verify order contents")
    void shouldDisplayOrderItemsInDetails() {
        // Given: I am viewing the orders list
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        
        // When: I click on an order that has items
        page.locator(".order-card").first().click();
        assertThat(page.locator("#order-modal")).hasClass("modal show");
        
        // Then: Order items should be displayed
        assertThat(page.locator("#order-details")).containsText("Order Items:");
        
        // And: If items exist, they should show proper details
        Locator itemsList = page.locator(".order-items-list");
        if (itemsList.locator(".order-item-detail").count() > 0) {
            Locator firstItem = itemsList.locator(".order-item-detail").first();
            assertThat(firstItem).containsText("Quantity:");
            assertThat(firstItem).containsText("Unit Price:");
            assertThat(firstItem).containsText("Total:");
        }
    }

    @Test
    @Order(6)
    @DisplayName("As a user, I want to create an order with multiple items, so that I can purchase several products at once")
    void shouldCreateOrderWithMultipleItems() {
        // Given: I am on the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        // When: I add multiple items to the order
        page.fill("#user-id", "888");
        page.fill("#shipping-address", "456 Multi-Item Ave, Test City, TC 67890");
        
        // Fill first item
        Locator firstItem = page.locator(".order-item").first();
        firstItem.locator("input[name='productId']").fill("2001");
        firstItem.locator("input[name='productName']").fill("Product A");
        firstItem.locator("input[name='quantity']").fill("1");
        firstItem.locator("input[name='unitPrice']").fill("15.50");
        
        // Add and fill second item
        page.click("#add-item-btn");
        Locator secondItem = page.locator(".order-item").nth(1);
        secondItem.locator("input[name='productId']").fill("2002");
        secondItem.locator("input[name='productName']").fill("Product B");
        secondItem.locator("input[name='quantity']").fill("3");
        secondItem.locator("input[name='unitPrice']").fill("22.75");
        
        // Add and fill third item
        page.click("#add-item-btn");
        Locator thirdItem = page.locator(".order-item").nth(2);
        thirdItem.locator("input[name='productId']").fill("2003");
        thirdItem.locator("input[name='productName']").fill("Product C");
        thirdItem.locator("input[name='quantity']").fill("2");
        thirdItem.locator("input[name='unitPrice']").fill("8.99");
        
        // And: I submit the form
        page.click("button[type='submit']");
        
        // Then: A success message should appear
        page.waitForSelector(".success", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertThat(page.locator(".success")).isVisible();
        assertThat(page.locator(".success")).containsText("Order created successfully");
    }

    @Test
    @Order(7)
    @DisplayName("As a user, I want to see clear error messages for failed operations, so that I understand what went wrong")
    void shouldDisplayErrorMessagesForFailedOperations() {
        // Given: I am on the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        // When: I try to create an order with invalid data (negative quantity)
        page.fill("#user-id", "777");
        page.fill("#shipping-address", "Invalid Order Test");
        
        Locator firstItem = page.locator(".order-item").first();
        firstItem.locator("input[name='productId']").fill("9999");
        firstItem.locator("input[name='productName']").fill("Invalid Product");
        firstItem.locator("input[name='quantity']").fill("0"); // Invalid quantity
        firstItem.locator("input[name='unitPrice']").fill("10.00");
        
        // And: I submit the form
        page.click("button[type='submit']");
        
        // Then: HTML5 validation should prevent submission
        // The quantity field should show validation error for min="1"
        String validationMessage = page.locator("input[name='quantity']").first()
                .evaluate("el => el.validationMessage").toString();
        assertTrue(!validationMessage.isEmpty() || 
                  page.locator("input[name='quantity']").first().getAttribute("min").equals("1"),
                  "Quantity field should have minimum validation");
    }

    @Test
    @Order(8)
    @DisplayName("As a user, I want to see proper form validation for required fields, so that I provide complete information")
    void shouldValidateRequiredFields() {
        // Given: I am on the create order form
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        
        // When: I try to submit with missing required fields
        page.click("button[type='submit']");
        
        // Then: Required field validation should prevent submission
        assertThat(page.locator("#user-id")).hasAttribute("required", "");
        assertThat(page.locator("#shipping-address")).hasAttribute("required", "");
        assertThat(page.locator("input[name='productId']")).hasAttribute("required", "");
        assertThat(page.locator("input[name='productName']")).hasAttribute("required", "");
        assertThat(page.locator("input[name='quantity']")).hasAttribute("required", "");
        assertThat(page.locator("input[name='unitPrice']")).hasAttribute("required", "");
    }

    @Test
    @Order(9)
    @DisplayName("As a user, I want to see orders with correct date formatting, so that I can understand when orders were placed")
    void shouldDisplayCorrectDateFormatting() {
        // Given: I am viewing orders
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        
        // When: I view order dates
        Locator orderCards = page.locator(".order-card");
        
        // Then: Order dates should be properly formatted
        for (int i = 0; i < Math.min(3, orderCards.count()); i++) {
            Locator orderCard = orderCards.nth(i);
            Locator dateElement = orderCard.locator(".order-info").filter(
                new Locator.FilterOptions().setHasText("Order Date:")
            ).locator("span");
            
            String dateText = dateElement.textContent();
            assertTrue(!dateText.isEmpty(), "Order date should not be empty");
            // Basic check that it looks like a date (contains numbers and common date separators)
            assertTrue(dateText.matches(".*\\d.*"), "Order date should contain numbers");
        }
    }

    @Test
    @Order(10)
    @DisplayName("As a user, I want to see monetary amounts properly formatted, so that I can understand costs clearly")
    void shouldDisplayCorrectMonetaryFormatting() {
        // Given: I am viewing orders
        page.navigate(baseUrl);
        page.waitForSelector(".order-card");
        
        // When: I view order amounts
        Locator orderCards = page.locator(".order-card");
        
        // Then: Monetary amounts should be properly formatted
        for (int i = 0; i < Math.min(3, orderCards.count()); i++) {
            Locator orderCard = orderCards.nth(i);
            Locator amountElement = orderCard.locator(".order-info").filter(
                new Locator.FilterOptions().setHasText("Total Amount:")
            ).locator("span");
            
            String amountText = amountElement.textContent();
            assertTrue(amountText.startsWith("$"), "Amount should start with $");
            assertTrue(amountText.matches("\\$\\d+(\\.\\d{2})?"), 
                "Amount should be properly formatted currency");
        }
    }

    @Test
    @Order(11)
    @DisplayName("As a user, I want the interface to be keyboard accessible, so that I can navigate without a mouse")
    void shouldSupportKeyboardNavigation() {
        // Given: I am on the order management system
        page.navigate(baseUrl);
        
        // When: I use keyboard navigation
        page.keyboard().press("Tab");
        
        // Then: Focus should move to interactive elements
        // Check that buttons can receive focus
        page.focus("#view-orders-btn");
        assertThat(page.locator("#view-orders-btn")).isFocused();
        
        page.keyboard().press("Tab");
        assertThat(page.locator("#create-order-btn")).isFocused();
        
        // Navigate to create order page using keyboard
        page.keyboard().press("Enter");
        assertThat(page.locator("#create-section")).hasClass("section active");
        
        // Test form field navigation
        page.focus("#user-id");
        assertThat(page.locator("#user-id")).isFocused();
        
        page.keyboard().press("Tab");
        assertThat(page.locator("#shipping-address")).isFocused();
    }

    @Test
    @Order(12)
    @DisplayName("As a user, I want consistent UI styling throughout the application, so that I have a cohesive experience")
    void shouldMaintainConsistentStyling() {
        // Given: I am on the order management system
        page.navigate(baseUrl);
        
        // When: I navigate through different sections
        
        // Check header consistency
        assertThat(page.locator("header")).isVisible();
        assertThat(page.locator("header h1")).hasText("Order Management System");
        
        // Check navigation button styling
        assertThat(page.locator(".nav-btn")).not().hasCount(0);
        assertThat(page.locator(".nav-btn.active")).hasCount(1);
        
        // Navigate to create order
        page.click("#create-order-btn");
        
        // Check form styling consistency
        assertThat(page.locator(".form-input")).not().hasCount(0);
        assertThat(page.locator(".btn")).not().hasCount(0);
        
        // Check that footer is present and styled
        assertThat(page.locator("footer")).isVisible();
        assertThat(page.locator("footer")).containsText("Order Management System");
    }

    @Test
    @Order(13)
    @DisplayName("As a user, I want to clear filters easily, so that I can see all orders again")
    void shouldClearFiltersToShowAllOrders() {
        // Given: I have applied filters
        page.navigate(baseUrl);
        page.waitForSelector("#orders-list");
        
        page.fill("#user-filter", "1");
        page.selectOption("#status-filter", "DELIVERED");
        page.click("#apply-filters-btn");
        page.waitForTimeout(500);
        
        int filteredCount = page.locator(".order-card").count();
        
        // When: I clear the filters
        page.fill("#user-filter", "");
        page.selectOption("#status-filter", "");
        page.click("#apply-filters-btn");
        page.waitForTimeout(500);
        
        // Then: All orders should be displayed again
        int unfilteredCount = page.locator(".order-card").count();
        assertTrue(unfilteredCount >= filteredCount, 
            "Unfiltered list should have at least as many orders as filtered list");
    }

    @Test
    @Order(14)
    @DisplayName("As a user, I want tooltips and helpful UI indicators, so that I understand how to use the interface")
    void shouldProvideHelpfulUIIndicators() {
        // Given: I am on the order management system
        page.navigate(baseUrl);
        
        // When: I interact with various UI elements
        
        // Check that buttons have meaningful text
        assertThat(page.locator("#view-orders-btn")).hasText("View Orders");
        assertThat(page.locator("#create-order-btn")).hasText("Create Order");
        assertThat(page.locator("#apply-filters-btn")).hasText("Apply Filters");
        
        // Navigate to create order and check form labels
        page.click("#create-order-btn");
        
        // Check that form fields have clear labels
        assertThat(page.locator("label[for='user-id']")).hasText("User ID:");
        assertThat(page.locator("label[for='shipping-address']")).hasText("Shipping Address:");
        
        // Check that placeholders provide guidance
        assertThat(page.locator("input[name='productId']")).hasAttribute("placeholder", "Product ID");
        assertThat(page.locator("input[name='productName']")).hasAttribute("placeholder", "Product Name");
        assertThat(page.locator("input[name='quantity']")).hasAttribute("placeholder", "Quantity");
        assertThat(page.locator("input[name='unitPrice']")).hasAttribute("placeholder", "Unit Price");
    }

    @Test
    @Order(15)
    @DisplayName("As a user, I want the application to work reliably across browser refreshes, so that my data persists")
    void shouldMaintainStateAcrossBrowserRefresh() {
        // Given: I am on the create order page
        page.navigate(baseUrl);
        page.click("#create-order-btn");
        assertThat(page.locator("#create-section")).hasClass("section active");
        
        // When: I refresh the browser
        page.reload();
        
        // Then: The page should load correctly
        assertThat(page).hasTitle("Order Management System");
        assertThat(page.locator("h1")).hasText("Order Management System");
        
        // And: I should be able to navigate normally
        assertThat(page.locator("#view-orders-btn")).isVisible();
        assertThat(page.locator("#create-order-btn")).isVisible();
        
        // Default view should be orders list
        assertThat(page.locator("#orders-section")).hasClass("section active");
    }
}