package com.orderplatform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * The single front door. Routes /api/products/** to inventory and /api/orders/** to the order
 * service, rewriting the /api prefix away. Optional Redis-backed rate limiting (Phase 5) is
 * enabled with {@code gateway.rate-limit.enabled=true} (set in docker-compose, where Redis runs).
 */
@Configuration
public class GatewayConfig {

    @Value("${services.inventory.uri:http://localhost:8081}")
    private String inventoryUri;

    @Value("${services.order.uri:http://localhost:8082}")
    private String orderUri;

    @Value("${services.payment.uri:http://localhost:8083}")
    private String paymentUri;

    @Value("${gateway.rate-limit.enabled:false}")
    private boolean rateLimitEnabled;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               org.springframework.beans.factory.ObjectProvider<RedisRateLimiter> limiter,
                               org.springframework.beans.factory.ObjectProvider<KeyResolver> keyResolver) {
        return builder.routes()
                .route("products", r -> r.path("/api/products/**")
                        .filters(f -> {
                            f.rewritePath("/api/(?<seg>.*)", "/${seg}");
                            applyRateLimit(f, limiter, keyResolver);
                            return f;
                        })
                        .uri(inventoryUri))
                .route("orders", r -> r.path("/api/orders/**")
                        .filters(f -> {
                            f.rewritePath("/api/(?<seg>.*)", "/${seg}");
                            applyRateLimit(f, limiter, keyResolver);
                            return f;
                        })
                        .uri(orderUri))
                .route("payments", r -> r.path("/api/payments/**")
                        .filters(f -> {
                            f.rewritePath("/api/(?<seg>.*)", "/${seg}");
                            applyRateLimit(f, limiter, keyResolver);
                            return f;
                        })
                        .uri(paymentUri))
                .build();
    }

    private void applyRateLimit(org.springframework.cloud.gateway.route.builder.GatewayFilterSpec f,
                                org.springframework.beans.factory.ObjectProvider<RedisRateLimiter> limiter,
                                org.springframework.beans.factory.ObjectProvider<KeyResolver> keyResolver) {
        if (rateLimitEnabled) {
            f.requestRateLimiter(c -> c.setRateLimiter(limiter.getObject())
                    .setKeyResolver(keyResolver.getObject()));
        }
    }

    /** Per-user (or per-IP for anonymous) rate-limit key. */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (user != null) {
                return Mono.just(user);
            }
            var remote = exchange.getRequest().getRemoteAddress();
            return Mono.just(remote != null ? remote.getAddress().getHostAddress() : "anonymous");
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter(
            @Value("${gateway.rate-limit.replenish-rate:10}") int replenishRate,
            @Value("${gateway.rate-limit.burst-capacity:20}") int burstCapacity) {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }
}
