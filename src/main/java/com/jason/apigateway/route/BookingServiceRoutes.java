package com.jason.apigateway.route;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;

@Configuration
public class BookingServiceRoutes {

    public static final String BOOKING_SERVICE_CIRCUIT_BREAKER_NAME = "bookingServiceCircuitBreaker";
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public BookingServiceRoutes(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Bean
    public RouterFunction<ServerResponse> bookingRoutes() {
        // need to register the circuit breaker with the registry or actuator doesn't see it
        circuitBreakerRegistry.circuitBreaker(BOOKING_SERVICE_CIRCUIT_BREAKER_NAME);

        return GatewayRouterFunctions.route("booking-service")
                .route(
                        RequestPredicates.POST("/api/v1/booking"),
                        HandlerFunctions.http("http://localhost:8081/api/v1/booking")
                )
                .filter(
                        CircuitBreakerFilterFunctions.circuitBreaker(
                                BOOKING_SERVICE_CIRCUIT_BREAKER_NAME,
                                URI.create("forward:/fallbackRoute")
                        )
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> fallbackRoute() {
        return GatewayRouterFunctions.route("fallbackRoute")
                .POST(
                        "/fallbackRoute",
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body("Booking service is down")
                )
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> bookingServiceApiDocs() {
        return GatewayRouterFunctions.route("booking-service-api-docs")
                .route(RequestPredicates.path("/docs/bookingservice/v3/api-docs"),
                        HandlerFunctions.http("http://localhost:8081"))
                // replace http://localhost:8081/docs/bookingservice/v3/api-docs -> http://localhost:8081/v3/api-docs
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}
