package com.agentportal.machine;

import com.agentportal.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Mode parsing, role mapping, and privilege ceiling checks.
 */
@Service
public class MachineModeService {

    private final AppProperties appProperties;

    public MachineModeService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public MachineMode resolveEffectiveMode(String requestedMode, String headerMaxMode) {
        MachineMode requested;
        try {
            requested = MachineMode.parse(requestedMode);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown machine mode: " + requestedMode + " (use observe|advise|act|ops)");
        }

        MachineMode ceiling = ceilingFrom(headerMaxMode, appProperties.getMachineGateway().getMaxMode());
        if (!requested.isAtMost(ceiling)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Mode " + requested.name().toLowerCase() + " exceeds max mode "
                            + ceiling.name().toLowerCase());
        }
        return requested;
    }

    private MachineMode ceilingFrom(String headerMaxMode, String configuredMaxMode) {
        MachineMode fromConfig;
        try {
            fromConfig = MachineMode.parse(configuredMaxMode);
        } catch (IllegalArgumentException e) {
            fromConfig = MachineMode.ACT;
        }
        if (headerMaxMode == null || headerMaxMode.isBlank()) {
            return fromConfig;
        }
        MachineMode fromHeader;
        try {
            fromHeader = MachineMode.parse(headerMaxMode);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid X-Machine-Max-Mode: " + headerMaxMode);
        }
        // Effective ceiling is the lower of header and config (cannot raise via header alone)
        return fromHeader.ordinal() <= fromConfig.ordinal() ? fromHeader : fromConfig;
    }
}
