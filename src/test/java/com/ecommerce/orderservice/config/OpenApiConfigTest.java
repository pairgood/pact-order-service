package com.ecommerce.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
public class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @Test
    void orderServiceOpenAPI_ShouldReturnConfiguredOpenAPI() {
        // When
        OpenAPI openAPI = openApiConfig.orderServiceOpenAPI();

        // Then
        assertThat(openAPI).isNotNull();
    }

    @Test
    void orderServiceOpenAPI_ShouldHaveCorrectInfo() {
        // When
        OpenAPI openAPI = openApiConfig.orderServiceOpenAPI();
        Info info = openAPI.getInfo();

        // Then
        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("Order Service API");
        assertThat(info.getVersion()).isEqualTo("1.0");
        assertThat(info.getDescription()).contains("order management");
        assertThat(info.getDescription()).contains("e-commerce microservices");
    }

    @Test
    void orderServiceOpenAPI_ShouldHaveCorrectContact() {
        // When
        OpenAPI openAPI = openApiConfig.orderServiceOpenAPI();
        Contact contact = openAPI.getInfo().getContact();

        // Then
        assertThat(contact).isNotNull();
        assertThat(contact.getEmail()).isEqualTo("support@ecommerce.com");
        assertThat(contact.getName()).isEqualTo("E-Commerce Support");
        assertThat(contact.getUrl()).isEqualTo("https://www.ecommerce.com");
    }

    @Test
    void orderServiceOpenAPI_ShouldHaveCorrectLicense() {
        // When
        OpenAPI openAPI = openApiConfig.orderServiceOpenAPI();
        License license = openAPI.getInfo().getLicense();

        // Then
        assertThat(license).isNotNull();
        assertThat(license.getName()).isEqualTo("MIT License");
        assertThat(license.getUrl()).isEqualTo("https://choosealicense.com/licenses/mit/");
    }

    @Test
    void orderServiceOpenAPI_ShouldHaveCorrectServer() {
        // When
        OpenAPI openAPI = openApiConfig.orderServiceOpenAPI();

        // Then
        assertThat(openAPI.getServers()).isNotNull();
        assertThat(openAPI.getServers()).hasSize(1);

        Server server = openAPI.getServers().get(0);
        assertThat(server.getUrl()).isEqualTo("http://localhost:8083");
        assertThat(server.getDescription()).isEqualTo("Server URL in Development environment");
    }

    @Test
    void orderServiceOpenAPI_ShouldBeConfigurationBean() {
        // Verify that the method creates a new instance each time
        OpenAPI openAPI1 = openApiConfig.orderServiceOpenAPI();
        OpenAPI openAPI2 = openApiConfig.orderServiceOpenAPI();

        // Different instances but same configuration
        assertThat(openAPI1).isNotSameAs(openAPI2);
        assertThat(openAPI1.getInfo().getTitle()).isEqualTo(openAPI2.getInfo().getTitle());
    }

    @Test
    void orderServiceOpenAPI_ShouldHaveCompleteConfiguration() {
        // When
        OpenAPI openAPI = openApiConfig.orderServiceOpenAPI();

        // Then - Verify all required components are present
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isNotEmpty();
        assertThat(openAPI.getInfo().getVersion()).isNotEmpty();
        assertThat(openAPI.getInfo().getDescription()).isNotEmpty();
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getLicense()).isNotNull();
        assertThat(openAPI.getServers()).isNotEmpty();
    }

    @Test
    void orderServiceOpenAPI_InfoShouldContainKeywords() {
        // When
        OpenAPI openAPI = openApiConfig.orderServiceOpenAPI();
        String description = openAPI.getInfo().getDescription();

        // Then
        assertThat(description).containsIgnoringCase("order");
        assertThat(description).containsIgnoringCase("management");
        assertThat(description).containsIgnoringCase("creation");
        assertThat(description).containsIgnoringCase("processing");
        assertThat(description).containsIgnoringCase("status");
        assertThat(description).containsIgnoringCase("history");
        assertThat(description).containsIgnoringCase("User");
        assertThat(description).containsIgnoringCase("Product");
    }
}