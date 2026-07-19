package com.agentportal.security;

import com.agentportal.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    @Test
    void forgeCityRemainsRateLimitedWhenPortalIsOpenAccess() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setOpenAccess(true);
        RateLimitFilter filter = new RateLimitFilter(10, properties);
        var chain = mock(jakarta.servlet.FilterChain.class);
        MockHttpServletResponse lastResponse = null;

        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/integrations/forgecity/tamil-rewrite");
            request.setRemoteAddr("203.0.113.8");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, chain);
        }

        assertEquals(429, lastResponse.getStatus());
        assertEquals("no-store", lastResponse.getHeader("Cache-Control"));
        assertEquals("{\"status\":\"busy\"}", lastResponse.getContentAsString());
        verify(chain, times(10)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

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
