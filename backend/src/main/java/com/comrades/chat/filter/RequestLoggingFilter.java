package com.comrades.chat.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    // Пути, которые не логируем (шумные)
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/ws"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Пропускаем шумные пути
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        // Оборачиваем для кэширования тела
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String requestBody = getRequestBody(wrappedRequest);
            String responseBody = getResponseBody(wrappedResponse);

            log.info("→ {} {} | status={} | duration={}ms | request={} | response={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    wrappedResponse.getStatus(),
                    duration,
                    truncate(requestBody, 500),
                    truncate(responseBody, 500));

            // ВАЖНО: копируем тело обратно в реальный response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(uri::startsWith);
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) return "";
        return new String(content, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) return "";
        return new String(content, StandardCharsets.UTF_8);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }
}