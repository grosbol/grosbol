package com.example.confluence.sso.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SSOConfigTest {

    @Test
    void defaultsArePopulatedByBuilder() {
        SSOConfig cfg = SSOConfig.builder().build();
        assertFalse(cfg.isEnabled());
        assertFalse(cfg.isForceSSO());
        assertEquals("/", cfg.getDefaultRedirectPath());
        assertTrue(cfg.isAutoProvisionUsers());
        assertFalse(cfg.isSyncGroupMembership());
        assertEquals("confluence-users", cfg.getDefaultGroup());
    }

    @Test
    void builderOverridesDefaults() {
        SSOConfig cfg = SSOConfig.builder()
            .enabled(true)
            .forceSSO(true)
            .defaultRedirectPath("/home")
            .autoProvisionUsers(false)
            .syncGroupMembership(true)
            .defaultGroup("my-group")
            .build();

        assertTrue(cfg.isEnabled());
        assertTrue(cfg.isForceSSO());
        assertEquals("/home", cfg.getDefaultRedirectPath());
        assertFalse(cfg.isAutoProvisionUsers());
        assertTrue(cfg.isSyncGroupMembership());
        assertEquals("my-group", cfg.getDefaultGroup());
    }
}
