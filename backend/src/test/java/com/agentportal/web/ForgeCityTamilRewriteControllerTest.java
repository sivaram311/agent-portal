package com.agentportal.web;

import com.agentportal.config.ForgeCityRewriteProperties;
import com.agentportal.integration.forgecity.ForgeCityTamilRewriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ForgeCityTamilRewriteControllerTest {

    @Test
    void globalPortalApiKeyDoesNotAuthenticateEndpoint() {
        ForgeCityRewriteProperties properties = enabledProperties();
        ForgeCityTamilRewriteService service = mock(ForgeCityTamilRewriteService.class);
        ForgeCityTamilRewriteController controller =
                new ForgeCityTamilRewriteController(properties, service, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "dedicated-secret");

        var response = controller.rewrite(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(Map.of("status", "unauthorized"), response.getBody());
        verifyNoInteractions(service);
    }

    @Test
    void disabledEndpointIsUnavailableEvenWithKey() {
        ForgeCityRewriteProperties properties = enabledProperties();
        properties.setEnabled(false);
        ForgeCityTamilRewriteService service = mock(ForgeCityTamilRewriteService.class);
        ForgeCityTamilRewriteController controller =
                new ForgeCityTamilRewriteController(properties, service, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-ForgeCity-Key", "dedicated-secret");

        var response = controller.rewrite(request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(Map.of("status", "unavailable"), response.getBody());
        verifyNoInteractions(service);
    }

    @Test
    void validDedicatedRequestReturnsOnlyContractResponse() throws Exception {
        ForgeCityRewriteProperties properties = enabledProperties();
        ForgeCityTamilRewriteService service = mock(ForgeCityTamilRewriteService.class);
        when(service.rewrite(any())).thenReturn("பேருந்து தாமதம்");
        ForgeCityTamilRewriteController controller =
                new ForgeCityTamilRewriteController(properties, service, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-ForgeCity-Key", "dedicated-secret");
        request.setContent("""
                {"schemaVersion":1,"appLabel":"ForgeCity","title":"Alert","text":"Bus delayed","maxChars":120}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var response = controller.rewrite(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals(new ForgeCityTamilRewriteController.RewriteResponse(
                1, "ok", "பேருந்து தாமதம்"), response.getBody());
        verify(service).rewrite(any());
    }

    @Test
    void oversizedBodyIsRejectedBeforeService() {
        ForgeCityRewriteProperties properties = enabledProperties();
        ForgeCityTamilRewriteService service = mock(ForgeCityTamilRewriteService.class);
        ForgeCityTamilRewriteController controller =
                new ForgeCityTamilRewriteController(properties, service, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-ForgeCity-Key", "dedicated-secret");
        request.setContent(new byte[16_385]);

        var response = controller.rewrite(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("status", "invalid_request"), response.getBody());
        verifyNoInteractions(service);
    }

    private static ForgeCityRewriteProperties enabledProperties() {
        ForgeCityRewriteProperties properties = new ForgeCityRewriteProperties();
        properties.setEnabled(true);
        properties.setApiKey("dedicated-secret");
        return properties;
    }
}
