package com.univus.app.security;

import tools.jackson.databind.ObjectMapper;
import com.univus.app.subscription.dto.SubscriptionAccessStatusDto;
import com.univus.app.subscription.service.SubscriptionAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubscriptionAccessInterceptor implements HandlerInterceptor {

    private final SubscriptionAccessService subscriptionAccessService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long memberId)) {
            return true;
        }

        SubscriptionAccessStatusDto status =
                subscriptionAccessService.getStatus(memberId);

        if (status.isServiceAccessible() || "SUA".equals(status.getRole())) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "success", false,
                "code", "SUBSCRIPTION_EXPIRED",
                "message", "The university subscription has ended.",
                "role", status.getRole(),
                "accessStatus", status.getAccessStatus(),
                "resubscribeAvailable", status.isResubscribeAvailable()
        ));
        return false;
    }
}
