package uz.coder.api_gateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig {

    @Bean
    fun routes(builder: RouteLocatorBuilder): RouteLocator = builder.routes()
        .route("keycloak") { r ->
            r.path("/auth/**")
                .filters { f -> f.rewritePath("/auth/(?<segment>.*)", "/$\\{segment}") }
                .uri("http://keycloak:8080")
        }
        .route("order-service") { r ->
            r.path("/api/orders/**")
                .filters { f -> f.addRequestHeader("X-Source", "api-gateway") }
                .uri("lb://order-service")
        }
        .route("inventory-service") { r ->
            r.path("/api/inventory/**")
                .filters { f -> f.addRequestHeader("X-Source", "api-gateway") }
                .uri("lb://inventory-service")
        }
        .build()
}