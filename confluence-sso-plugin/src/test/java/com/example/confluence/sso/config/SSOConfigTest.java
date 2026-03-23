package com.example.confluence.sso.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SSOConfigTest {

    @Test
    void defaultsArePopulatedByBuilder() {
        SSOConfig cfg = SSOConfig.builder().build();

        assertEquals(SSOConfig.Protocol.SAML, cfg.getProtocol());
        assertFalse(cfg.isEnabled());
        assertFalse(cfg.isForceSSO());
        assertEquals("/", cfg.getDefaultRedirectPath());
        assertEquals("email", cfg.getSamlAttributeEmail());
        assertEquals("displayName", cfg.getSamlAttributeFullName());
        assertEquals("groups", cfg.getSamlAttributeGroups());
        assertTrue(cfg.isSamlSignRequests());
        assertTrue(cfg.isSamlWantAssertionsSigned());
        assertEquals("openid profile email", cfg.getOidcScopes());
        assertTrue(cfg.isOidcPkceEnabled());
        assertTrue(cfg.isAutoProvisionUsers());
        assertFalse(cfg.isSyncGroupMembership());
        assertEquals("confluence-users", cfg.getDefaultGroup());
    }

    @Test
    void builderOverridesDefaults() {
        SSOConfig cfg = SSOConfig.builder()
            .protocol(SSOConfig.Protocol.OIDC)
            .enabled(true)
            .forceSSO(true)
            .samlSignRequests(false)
            .autoProvisionUsers(false)
            .syncGroupMembership(true)
            .defaultGroup("my-users")
            .build();

        assertEquals(SSOConfig.Protocol.OIDC, cfg.getProtocol());
        assertTrue(cfg.isEnabled());
        assertTrue(cfg.isForceSSO());
        assertFalse(cfg.isSamlSignRequests());
        assertFalse(cfg.isAutoProvisionUsers());
        assertTrue(cfg.isSyncGroupMembership());
        assertEquals("my-users", cfg.getDefaultGroup());
    }

    @Test
    void oidcFields() {
        SSOConfig cfg = SSOConfig.builder()
            .oidcIssuerUrl("https://accounts.google.com")
            .oidcClientId("cid")
            .oidcClientSecret("csec")
            .oidcRedirectUri("https://conf.example.com/cb")
            .oidcPkceEnabled(false)
            .build();

        assertEquals("https://accounts.google.com", cfg.getOidcIssuerUrl());
        assertEquals("cid",  cfg.getOidcClientId());
        assertEquals("csec", cfg.getOidcClientSecret());
        assertFalse(cfg.isOidcPkceEnabled());
    }
}
