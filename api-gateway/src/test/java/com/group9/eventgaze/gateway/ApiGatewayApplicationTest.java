package com.group9.eventgaze.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("Context loads and routes are properly registered")
    void contextLoadsAndRoutesRegistered() {
        assertNotNull(routeLocator, "RouteLocator should be present in the application context");

        List<String> routeIds = routeLocator.getRoutes()
                .map(org.springframework.cloud.gateway.route.Route::getId)
                .collectList()
                .block();

        assertNotNull(routeIds);
        assertTrue(routeIds.contains("auth-service"), "Route auth-service should be configured");
        assertTrue(routeIds.contains("event-service-events"), "Route event-service-events should be configured");
        assertTrue(routeIds.contains("event-service-category"), "Route event-service-category should be configured");
        assertTrue(routeIds.contains("event-service-collegelist"), "Route event-service-collegelist should be configured");
        assertTrue(routeIds.contains("bookmark-service"), "Route bookmark-service should be configured");
        assertTrue(routeIds.contains("auth-docs"), "Route auth-docs should be configured");
        assertTrue(routeIds.contains("event-docs"), "Route event-docs should be configured");
        assertTrue(routeIds.contains("bookmark-docs"), "Route bookmark-docs should be configured");
    }
}
