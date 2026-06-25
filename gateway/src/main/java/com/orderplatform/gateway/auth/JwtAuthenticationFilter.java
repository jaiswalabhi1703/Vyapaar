package com.orderplatform.gateway.auth;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the JWT at the edge and forwards the authenticated identity downstream as
 * {@code X-User-Id}. Unauthenticated requests to protected routes are rejected here, so no
 * business service ever sees them. Public: /auth/**, browsing products (GET /api/products/**),
 * and actuator.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwt;

    public JwtAuthenticationFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Resolve identity if a valid token is present (even on public routes, so admins browsing
        // the catalog get their role forwarded and can see stock).
        String auth = request.getHeaders().getFirst("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        Claims claims = token == null ? null : jwt.validate(token);

        if (claims == null) {
            if (isPublic(request)) {
                // Anonymous public request: strip any spoofed identity headers and pass through.
                return chain.filter(stripUserHeaders(exchange));
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String resolvedRole = role != null ? role : "USER";
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.set("X-User-Id", username);
                    h.set("X-User-Role", resolvedRole);
                }))
                .build();
        return chain.filter(mutated);
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (request.getMethod() == HttpMethod.OPTIONS) return true;
        if (path.startsWith("/auth/")) return true;
        if (path.startsWith("/actuator/")) return true;
        // Browsing the catalog is public; placing/reading orders is not.
        return request.getMethod() == HttpMethod.GET && path.startsWith("/api/products");
    }

    private ServerWebExchange stripUserHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                }))
                .build();
    }

    @Override
    public int getOrder() {
        return -1; // run before routing
    }
}
