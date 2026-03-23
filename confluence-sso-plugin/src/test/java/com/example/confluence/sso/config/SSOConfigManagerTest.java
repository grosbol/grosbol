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
import static org.mockito.ArgumentMatchers.anyString;
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
        doAnswer(inv -> store.get(inv.getArgument(0))).when(settings).get(anyString());
        doAnswer(inv -> { store.put(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(settings).put(anyString(), anyString());
        manager = new SSOConfigManager(factory);
    }

    @Test
    void defaultsWhenNothingStored() {
        SSOConfig cfg = manager.load();
        assertFalse(cfg.isEnabled());
        assertFalse(cfg.isForceSSO());
        assertEquals("/", cfg.getDefaultRedirectPath());
        assertTrue(cfg.isAutoProvisionUsers());
        assertFalse(cfg.isSyncGroupMembership());
        assertEquals("confluence-users", cfg.getDefaultGroup());
    }

    @Test
    void saveAndLoadRoundTrip() {
        SSOConfig original = SSOConfig.builder()
            .enabled(true)
            .forceSSO(true)
            .defaultRedirectPath("/dashboard")
            .autoProvisionUsers(false)
            .syncGroupMembership(true)
            .defaultGroup("sso-users")
            .build();

        manager.save(original);
        SSOConfig loaded = manager.load();

        assertTrue(loaded.isEnabled());
        assertTrue(loaded.isForceSSO());
        assertEquals("/dashboard", loaded.getDefaultRedirectPath());
        assertFalse(loaded.isAutoProvisionUsers());
        assertTrue(loaded.isSyncGroupMembership());
        assertEquals("sso-users", loaded.getDefaultGroup());
    }
}
