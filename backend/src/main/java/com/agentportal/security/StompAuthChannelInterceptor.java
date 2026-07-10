package com.agentportal.security;

import com.agentportal.config.CssProperties;
import com.agentportal.service.SessionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SESSION_TOPIC = Pattern.compile("^/topic/sessions/([0-9a-fA-F\\-]{36})$");

    private final CssProperties cssProperties;
    private final SessionService sessionService;

    public StompAuthChannelInterceptor(CssProperties cssProperties, @Lazy SessionService sessionService) {
        this.cssProperties = cssProperties;
        this.sessionService = sessionService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        if (!cssProperties.isEnabled()) {
            return message;
        }
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // STOMP runs on a broker thread — SecurityContextHolder is empty unless we bind the WS principal.
        Authentication stompUser = accessor.getUser() instanceof Authentication a ? a : null;
        boolean bound = false;
        if (stompUser != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(stompUser);
            bound = true;
        }

        try {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                Authentication auth = stompUser != null
                        ? stompUser
                        : SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) {
                    throw new AccessDeniedException("WebSocket authentication required");
                }
            }
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String dest = accessor.getDestination();
                if (dest == null) {
                    return message;
                }
                Matcher m = SESSION_TOPIC.matcher(dest);
                if (m.matches()) {
                    UUID sessionId = UUID.fromString(m.group(1));
                    if (!sessionService.canAccess(sessionId)) {
                        throw new AccessDeniedException("Not allowed to subscribe to session " + sessionId);
                    }
                }
            }
            return message;
        } finally {
            if (bound) {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
