package com.vitaliq.vitaliq_platform.security;

import com.vitaliq.vitaliq_platform.model.auth.ApiKey;
import com.vitaliq.vitaliq_platform.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

//@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Check for X-API-Key header
        String apiKeyHeader = request.getHeader("X-API-Key");

        if (apiKeyHeader != null && !apiKeyHeader.isEmpty()) {
            // Step 2: Try to find the API key in database
            var keyOpt = apiKeyRepository.findByKey(apiKeyHeader);

            if (keyOpt.isPresent()) {
                ApiKey apiKey = keyOpt.get();

                // Step 3: Validate the key
                if (isValidApiKey(apiKey)) {
                    // Step 4: Update last used timestamp
                    apiKey.setLastUsedAt(LocalDateTime.now());
                    apiKeyRepository.save(apiKey);

                    /// Step 5: Create authentication token with VitalIqUserDetails (includes ApiKey)
                    VitalIqUserDetails userDetails = new VitalIqUserDetails(apiKey.getUser(), apiKey);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    new ArrayList<>()
                            );

                    // Step 6: Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    logger.warn("Invalid or expired API key attempt");
                }
            } else {
                logger.warn("API key not found: " + apiKeyHeader);
            }
        }

        // Step 7: Continue to next filter (JwtFilter, SecurityConfig, Controller)
        filterChain.doFilter(request, response);
    }

    /**
     * Validate if the API key is usable
     * Checks: is it active? Not expired?
     */
    private boolean isValidApiKey(ApiKey apiKey) {
        // Check 1: Is the key active?
        if (!apiKey.getIsActive()) {
            logger.warn("API key is inactive");
            return false;
        }

        // Check 2: Is the key expired?
        if (apiKey.getExpiresAt() != null && LocalDateTime.now().isAfter(apiKey.getExpiresAt())) {
            logger.warn("API key is expired");
            return false;
        }

        return true;
    }
}