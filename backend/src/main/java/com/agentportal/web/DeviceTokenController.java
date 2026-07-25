package com.agentportal.web;

import com.agentportal.domain.DeviceToken;
import com.agentportal.dto.RegisterDeviceTokenRequest;
import com.agentportal.repo.DeviceTokenRepository;
import com.agentportal.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenController(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void register(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        String ownerUsername = CurrentUser.usernameOrAnonymous();
        DeviceToken device = deviceTokenRepository.findByToken(request.token()).orElseGet(DeviceToken::new);
        device.setToken(request.token());
        device.setOwnerUsername(ownerUsername);
        device.setPlatform(request.platform() == null || request.platform().isBlank() ? "android" : request.platform());
        deviceTokenRepository.save(device);
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unregister(@PathVariable String token) {
        // Derived delete-query methods (unlike save()/deleteById() on
        // SimpleJpaRepository) need an explicit surrounding transaction when
        // called directly from a controller with no service layer beneath it.
        deviceTokenRepository.deleteByToken(token);
    }
}
