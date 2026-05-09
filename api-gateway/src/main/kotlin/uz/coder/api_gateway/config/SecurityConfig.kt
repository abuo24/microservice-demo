package uz.coder.api_gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { auth ->
                auth
                    // K8s probes — must be unauthenticated
                    .pathMatchers("/actuator/health/**").permitAll()
                    // Keycloak proxy — token endpoint must be unauthenticated
                    .pathMatchers("/auth/**").permitAll()
                    // Swagger UI for downstream services forwarded through gateway
                    .pathMatchers(
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/api/orders/swagger-ui/**", "/api/orders/v3/api-docs/**",
                        "/api/inventory/swagger-ui/**", "/api/inventory/v3/api-docs/**"
                    ).permitAll()
                    // Everything else requires a valid JWT
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(keycloakJwtConverter())
                }
            }
            .build()

    @Bean
    fun keycloakJwtConverter(): ReactiveJwtAuthenticationConverter {
        val authoritiesConverter = ReactiveJwtGrantedAuthoritiesConverterAdapter(
            Converter<Jwt, Collection<GrantedAuthority>> { jwt ->
                @Suppress("UNCHECKED_CAST")
                val roles = jwt.getClaimAsMap("realm_access")?.get("roles") as? List<String>
                    ?: return@Converter emptyList()
                roles.map { SimpleGrantedAuthority("ROLE_$it") }
            }
        )
        return ReactiveJwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        }
    }
}