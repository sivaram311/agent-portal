package com.agentportal.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PlatformRegistryGatewayRoleTest {

    @Autowired
    private PlatformRegistryService platformRegistryService;

    @Test
    void getRoleResolvesGatewayRoles() {
        assertEquals("GATEWAY_OBSERVE", platformRegistryService.getRole("GATEWAY_OBSERVE").id());
        assertEquals("GATEWAY_ACT", platformRegistryService.getRole("gateway_act").id());
    }
}
