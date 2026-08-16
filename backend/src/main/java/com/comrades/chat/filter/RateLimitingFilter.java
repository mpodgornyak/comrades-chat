package com.comrades.chat.filter;

import com.comrades.chat.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Map<String, RateLimitConfig> RATE_LIMITED_PATHS = Map.of(
            "/api/v1/auth/login", new RateLimitConfig(5, 60),
            "/api/v1/auth/register", new RateLimitConfig(3, 60),
            "/api/v1/messages", new RateLimitConfig(100, 60),
            "/api/v1/health", new RateLimitConfig(10, 60)
    );

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // КЛЮЧЕВОЙ МОМЕНТ: инжектим HandlerExceptionResolver
    // @Qualifier нужен, чтобы получить именно тот, что обрабатывает @ExceptionHandler
    private final HandlerExceptionResolver exceptionResolver;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    public RateLimitingFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        RateLimitConfig config = findConfigForPath(path);

        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String bucketKey = clientIp + ":" + path;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(config));

        if (bucket.tryConsume(1)) {
            // Лимит не превышен — пропускаем
            filterChain.doFilter(request, response);
        } else {
            // Лимит превышен — бросаем исключение через resolver
            log.warn("Rate limit exceeded for {} on path {}", clientIp, path);

            RateLimitExceededException exception = new RateLimitExceededException(clientIp, path);

            // resolveException направит исключение в @ExceptionHandler
            exceptionResolver.resolveException(request, response, null, exception);
        }
    }

    private RateLimitConfig findConfigForPath(String path) {
        for (Map.Entry<String, RateLimitConfig> entry : RATE_LIMITED_PATHS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Bucket createBucket(RateLimitConfig config) {
        Bandwidth limit = Bandwidth.classic(
                config.capacity(),
                Refill.greedy(config.capacity(), Duration.ofSeconds(config.refillSeconds()))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record RateLimitConfig(int capacity, int refillSeconds) {}
}