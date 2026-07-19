package com.agentportal.web;

import com.agentportal.config.ForgeCityRewriteProperties;
import com.agentportal.integration.forgecity.ForgeCityKeyAuthenticator;
import com.agentportal.integration.forgecity.ForgeCityRewriteContract;
import com.agentportal.integration.forgecity.ForgeCityTamilRewriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestController
public class ForgeCityTamilRewriteController {

    static final String PATH = "/api/integrations/forgecity/tamil-rewrite";
    private static final int MAX_BODY_BYTES = 16_384;

    private final ForgeCityRewriteProperties properties;
    private final ForgeCityTamilRewriteService service;
    private final ObjectMapper mapper;

    public ForgeCityTamilRewriteController(
            ForgeCityRewriteProperties properties,
            ForgeCityTamilRewriteService service,
            ObjectMapper mapper
    ) {
        this.properties = properties;
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping(
            path = PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> rewrite(HttpServletRequest httpRequest) {
        if (!properties.isAvailable()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "unavailable");
        }
        if (!ForgeCityKeyAuthenticator.matches(
                httpRequest.getHeader("X-ForgeCity-Key"),
                properties.getApiKey())) {
            return error(HttpStatus.UNAUTHORIZED, "unauthorized");
        }

        final ForgeCityRewriteContract.Request request;
        try {
            byte[] body = httpRequest.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            request = ForgeCityRewriteContract.parse(mapper, body, properties.getMaxChars());
        } catch (IOException | ForgeCityRewriteContract.InvalidRequestException e) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request");
        }

        try {
            String tamil = service.rewrite(request);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(new RewriteResponse(1, "ok", tamil));
        } catch (ForgeCityTamilRewriteService.BusyException e) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "busy");
        } catch (TimeoutException e) {
            return error(HttpStatus.GATEWAY_TIMEOUT, "timeout");
        } catch (ForgeCityTamilRewriteService.RewriteFailedException e) {
            return error(HttpStatus.BAD_GATEWAY, "failed");
        } catch (RuntimeException e) {
            return error(HttpStatus.BAD_GATEWAY, "failed");
        }
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String value) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("status", value));
    }

    public record RewriteResponse(int schemaVersion, String status, String tamil) {
    }
}
