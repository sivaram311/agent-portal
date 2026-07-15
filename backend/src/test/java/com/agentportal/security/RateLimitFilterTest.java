package com.agentportal.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    @Test
    void usesForwardedIpWhenPeerIsLoopback() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        assertEquals("203.0.113.9", RateLimitFilter.resolveClientIp(req));
        assertTrue(RateLimitFilter.clientKey(req).startsWith("ip:203.0.113.9"));
    }

    @Test
    void ignoresForwardedIpFromUntrustedPeer() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.50");
        req.addHeader("X-Forwarded-For", "198.51.100.1");
        assertEquals("203.0.113.50", RateLimitFilter.resolveClientIp(req));
    }

    @Test
    void prefersCfConnectingIp() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("CF-Connecting-IP", "198.51.100.7");
        req.addHeader("X-Forwarded-For", "203.0.113.9");
        assertEquals("198.51.100.7", RateLimitFilter.resolveClientIp(req));
    }

    @Test
    void jwtClaimReadsSub() {
        // {"sub":"admin","aud":"agent-portal"} — unsigned payload only
        String payload = Base64Url("{\"sub\":\"admin\",\"aud\":\"agent-portal\"}");
        String jwt = "x." + payload + ".y";
        assertEquals("admin", RateLimitFilter.jwtClaim(jwt, "sub"));
    }

    private static String Base64Url(String json) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
