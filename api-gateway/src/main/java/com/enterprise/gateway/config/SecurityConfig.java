package com.enterprise.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Requires a valid OAuth2 JWT bearer token (client-credentials grant) on
 * every route except /actuator/health, matching how banks/merchants
 * authenticate server-to-server in real payment networks. The actual
 * authorization server (issuing tokens, managing client credentials) is
 * NOT included here — this config only validates tokens issued elsewhere.
 * Point `spring.security.oauth2.resourceserver.jwt.issuer-uri` (see
 * application.yml) at a real OAuth2/OIDC provider (Keycloak, Okta, Auth0,
 * AWS Cognito, etc.) to make this functional.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
