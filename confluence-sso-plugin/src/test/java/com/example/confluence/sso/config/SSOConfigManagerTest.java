package com.example.confluence.sso.config;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SSOConfigManagerTest {

    @Mock private PluginSettingsFactory factory;
    @Mock private PluginSettings        settings;

    private SSOConfigManager manager;
    private final Map<String, Object> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(factory.createGlobalSettings()).thenReturn(settings);

        // Simulate in-memory store
        doAnswer(inv -> store.get(inv.getArgument(0)))
            .when(settings).get(anyString());
        doAnswer(inv -> { store.put(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(settings).put(anyString(), anyString());

        manager = new SSOConfigManager(factory);
    }

    @Test
    void defaultsAreReturnedWhenNothingStored() {
        SSOConfig cfg = manager.load();
        assertFalse(cfg.isEnabled());
        assertEquals(SSOConfig.Protocol.SAML, cfg.getProtocol());
        assertEquals("email", cfg.getSamlAttributeEmail());
        assertTrue(cfg.isAutoProvisionUsers());
    }

    @Test
    void saveAndLoadRoundTrip() {
        SSOConfig original = SSOConfig.builder()
            .enabled(true)
            .forceSSO(true)
            .protocol(SSOConfig.Protocol.OIDC)
            .oidcIssuerUrl("https://accounts.google.com")
            .oidcClientId("my-client")
            .oidcClientSecret("s3cr3t")
            .oidcRedirectUri("https://confluence.example.com/plugins/servlet/sso/oidc/callback")
            .defaultGroup("my-group")
            .build();

        manager.save(original);
        SSOConfig loaded = manager.load();

        assertTrue(loaded.isEnabled());
        assertTrue(loaded.isForceSSO());
        assertEquals(SSOConfig.Protocol.OIDC, loaded.getProtocol());
        assertEquals("https://accounts.google.com", loaded.getOidcIssuerUrl());
        assertEquals("my-client",  loaded.getOidcClientId());
        assertEquals("my-group",   loaded.getDefaultGroup());
    }

    @Test
    void invalidProtocolDefaultsToSAML() {
        store.put("com.example.confluence.sso.protocol", "INVALID");
        SSOConfig cfg = manager.load();
        assertEquals(SSOConfig.Protocol.SAML, cfg.getProtocol());
    }

    @Test
    void samlConfigRoundTrip() {
        SSOConfig original = SSOConfig.builder()
            .protocol(SSOConfig.Protocol.SAML)
            .enabled(true)
            .samlIdpEntityId("https://idp.example.com")
            .samlIdpSsoUrl("https://idp.example.com/sso")
            .samlIdpCertificate("-----BEGIN CERTIFICATE-----\nMIIC...")
            .samlSpEntityId("https://confluence.example.com")
            .samlSpAcsUrl("https://confluence.example.com/plugins/servlet/sso/saml/acs")
            .samlSignRequests(true)
            .samlWantAssertionsSigned(true)
            .build();

        manager.save(original);
        SSOConfig loaded = manager.load();

        assertEquals("https://idp.example.com", loaded.getSamlIdpEntityId());
        assertEquals("https://idp.example.com/sso", loaded.getSamlIdpSsoUrl());
        assertTrue(loaded.isSamlSignRequests());
        assertTrue(loaded.isSamlWantAssertionsSigned());
    }
}
