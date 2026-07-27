package com.agentportal.service;

import com.agentportal.domain.DeviceToken;
import com.agentportal.repo.DeviceTokenRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Mobile push notification dispatch — sibling of {@link WebhookService} for the Android client.
 *
 * <p>Firebase is only initialized when {@code app.firebase.credentials-path} points at a real
 * service-account JSON file. Without it, this stays log-only (the original, deliberate no-op
 * behavior) — no crash, no exception on boot, just a debug log noting push is disabled.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String FIREBASE_APP_NAME = "agent-portal-push";

    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    private FirebaseApp firebaseApp;

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostConstruct
    void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("app.firebase.credentials-path not set; push notifications stay log-only");
            return;
        }
        Path path = Path.of(credentialsPath);
        if (!Files.isRegularFile(path)) {
            log.warn("app.firebase.credentials-path={} does not exist; push notifications stay log-only", credentialsPath);
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            firebaseApp = FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
            log.info("Firebase push notifications enabled (project credentials loaded from {})", credentialsPath);
        } catch (IOException e) {
            log.warn("Failed to initialize Firebase from {}; push notifications stay log-only: {}", credentialsPath, e.getMessage());
        }
    }

    public void notifyOwner(String ownerUsername, String eventType, String sessionId, Map<String, Object> payload) {
        if (ownerUsername == null || ownerUsername.isBlank()) {
            return;
        }
        List<DeviceToken> devices;
        try {
            devices = deviceTokenRepository.findByOwnerUsername(ownerUsername);
        } catch (Exception e) {
            log.debug("Push notification device lookup failed: {}", e.getMessage());
            return;
        }
        // Per-device, not per-batch: one stale/invalid token must not stop the
        // rest of this user's devices from being notified.
        for (DeviceToken device : devices) {
            try {
                sendToDevice(device, eventType, sessionId, payload);
            } catch (Exception e) {
                log.debug("Push notification send failed for a device: {}", e.getMessage());
            }
        }
    }

    private void sendToDevice(DeviceToken device, String eventType, String sessionId, Map<String, Object> payload) {
        String token = device.getToken();
        String truncated = token == null || token.length() <= 8
                ? "***"
                : token.substring(0, 4) + "..." + token.substring(token.length() - 4);

        if (firebaseApp == null) {
            log.info("Would push to device token {} for event {} on session {} (Firebase not configured)",
                    truncated, eventType, sessionId);
            return;
        }

        Message.Builder message = Message.builder()
                .setToken(token)
                .putData("sessionId", sessionId)
                .putData("eventType", eventType);

        // permission_required / plan_required carry a real PermissionRequest id
        // the app can act on (approve/reject); other events (run_completed etc.)
        // are informational only, no permissionId to attach.
        if (payload != null) {
            Object permissionId = payload.get("permissionId");
            if (permissionId != null) {
                message.putData("permissionId", permissionId.toString());
                message.putData("toolLabel", labelFor(eventType, payload));
                String detail = detailFor(eventType, payload);
                if (detail != null) {
                    message.putData("detail", detail);
                }
            }
        }

        try {
            FirebaseMessaging.getInstance(firebaseApp).send(message.build());
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                // Token is permanently invalid (app uninstalled, token rotated
                // elsewhere) -- stop trying it.
                deviceTokenRepository.deleteByToken(token);
                log.info("Removed stale/unregistered device token {}", truncated);
            } else {
                log.warn("FCM send failed for device token {}: {}", truncated, e.getMessage());
            }
        }
    }

    private String labelFor(String eventType, Map<String, Object> payload) {
        if ("plan_required".equals(eventType)) {
            Object name = payload.get("name");
            return name != null && !name.toString().isBlank() ? name.toString() : "Plan approval";
        }
        return "Tool permission";
    }

    private String detailFor(String eventType, Map<String, Object> payload) {
        Object raw = "plan_required".equals(eventType) ? payload.get("plan") : payload.get("details");
        if (raw == null) {
            return null;
        }
        String text = raw.toString();
        // FCM caps total data-message payload size (~4KB) -- keep this one
        // field well under that regardless of how large a tool-call's raw
        // params or a plan's markdown body might be.
        return text.length() > 500 ? text.substring(0, 500) + "…" : text;
    }
}
