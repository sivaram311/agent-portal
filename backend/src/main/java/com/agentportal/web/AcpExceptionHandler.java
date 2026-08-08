package com.agentportal.web;

import com.agentportal.acp.AcpStartTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class AcpExceptionHandler {

    @ExceptionHandler(AcpStartTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleAcpStartTimeout(AcpStartTimeoutException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "acp_start_timeout");
        body.put("message", ex.getMessage());
        body.put("sessionId", ex.getSessionId() == null ? null : ex.getSessionId().toString());
        body.put("waitedMs", ex.getWaitedMs());
        body.put("timeoutMs", ex.getTimeoutMs());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }
}
