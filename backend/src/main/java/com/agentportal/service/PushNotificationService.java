package com.agentportal.service;

import com.agentportal.domain.DeviceToken;
import com.agentportal.repo.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Mobile push notification dispatch — sibling of {@link WebhookService} for the Android client.
 *
 * <p>No Firebase project is configured yet (see docs/ROADMAP.md). This is a deliberate,
 * clearly-marked no-op/log-only send path: wiring in real FCM sending later means filling in
 * the body of {@link #sendToDevice} and adding the firebase-admin dependency, not a redesign.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final DeviceTokenRepository deviceTokenRepository;

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public void notifyOwner(String ownerUsername, String eventType, String sessionId, Map<String, Object> payload) {
        try {
            if (ownerUsername == null || ownerUsername.isBlank()) {
                return;
            }
            List<DeviceToken> devices = deviceTokenRepository.findByOwnerUsername(ownerUsername);
            if (devices.isEmpty()) {
                return;
            }
            for (DeviceToken device : devices) {
                sendToDevice(device, eventType, sessionId, payload);
            }
        } catch (Exception e) {
            log.debug("Push notification dispatch failed: {}", e.getMessage());
        }
    }

    /**
     * TODO(firebase): once a Firebase project exists, replace this log statement with a real
     * FCM send (add the firebase-admin dependency and call its messaging API here).
     */
    private void sendToDevice(DeviceToken device, String eventType, String sessionId, Map<String, Object> payload) {
        String token = device.getToken();
        String truncated = token == null || token.length() <= 8
                ? "***"
                : token.substring(0, 4) + "..." + token.substring(token.length() - 4);
        log.info("Would push to device token {} for event {} on session {} (no Firebase credentials configured -- see docs/ROADMAP.md)",
                truncated, eventType, sessionId);
    }
}
