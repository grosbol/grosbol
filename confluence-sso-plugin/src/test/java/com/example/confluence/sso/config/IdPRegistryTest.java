package com.example.confluence.sso.config;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdPRegistryTest {

    @Mock private PluginSettingsFactory factory;
    @Mock private PluginSettings        settings;

    private IdPRegistry registry;
    private final Map<String, Object> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(factory.createGlobalSettings()).thenReturn(settings);
        doAnswer(inv -> store.get(inv.getArgument(0))).when(settings).get(anyString());
        doAnswer(inv -> { store.put(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(settings).put(anyString(), anyString());
        doAnswer(inv -> { store.remove(inv.getArgument(0)); return null; })
            .when(settings).remove(anyString());
        registry = new IdPRegistry(factory);
    }

    @Test
    void emptyRegistryReturnsEmptyList() {
        assertTrue(registry.loadAll().isEmpty());
        assertTrue(registry.loadEnabled().isEmpty());
        assertNull(registry.defaultIfSingle());
    }

    @Test
    void saveAndReloadSingleIdP() {
        IdPConfig idp = IdPConfig.builder()
            .id("okta")
            .displayName("Okta SAML")
            .protocol(IdPConfig.Protocol.SAML)
            .samlIdpSsoUrl("https://okta.example.com/sso")
            .domainHint("corp.com")
            .build();

        registry.save(idp);
        List<IdPConfig> all = registry.loadAll();

        assertEquals(1, all.size());
        assertEquals("okta", all.get(0).getId());
        assertEquals("Okta SAML", all.get(0).getDisplayName());
        assertEquals("https://okta.example.com/sso", all.get(0).getSamlIdpSsoUrl());
    }

    @Test
    void defaultIfSingleReturnsSoleEnabledIdP() {
        registry.save(IdPConfig.builder().id("azure").displayName("Azure AD").enabled(true).build());
        IdPConfig def = registry.defaultIfSingle();
        assertNotNull(def);
        assertEquals("azure", def.getId());
    }

    @Test
    void defaultIfSingleReturnsNullForMultiple() {
        registry.save(IdPConfig.builder().id("a").displayName("A").enabled(true).build());
        registry.save(IdPConfig.builder().id("b").displayName("B").enabled(true).build());
        assertNull(registry.defaultIfSingle());
    }

    @Test
    void disabledIdPsAreExcludedFromLoadEnabled() {
        registry.save(IdPConfig.builder().id("active").displayName("Active").enabled(true).build());
        registry.save(IdPConfig.builder().id("inactive").displayName("Inactive").enabled(false).build());

        List<IdPConfig> enabled = registry.loadEnabled();
        assertEquals(1, enabled.size());
        assertEquals("active", enabled.get(0).getId());
    }

    @Test
    void domainHintMatchesCorrectIdP() {
        registry.save(IdPConfig.builder().id("idp1").displayName("Corp").domainHint("corp.com,corp.net").enabled(true).build());
        registry.save(IdPConfig.builder().id("idp2").displayName("Other").domainHint("other.com").enabled(true).build());

        IdPConfig found = registry.findByEmailDomain("user@corp.net");
        assertNotNull(found);
        assertEquals("idp1", found.getId());
    }

    @Test
    void domainHintReturnsNullForUnknownDomain() {
        registry.save(IdPConfig.builder().id("idp1").domainHint("corp.com").enabled(true).build());
        assertNull(registry.findByEmailDomain("user@other.com"));
    }

    @Test
    void deleteRemovesIdP() {
        registry.save(IdPConfig.builder().id("to-delete").displayName("X").build());
        registry.delete("to-delete");
        assertTrue(registry.loadAll().isEmpty());
        assertNull(registry.findById("to-delete"));
    }

    @Test
    void saveAllPreservesOrder() {
        List<IdPConfig> idps = List.of(
            IdPConfig.builder().id("first").displayName("First").build(),
            IdPConfig.builder().id("second").displayName("Second").build(),
            IdPConfig.builder().id("third").displayName("Third").build()
        );
        registry.saveAll(idps);

        List<IdPConfig> loaded = registry.loadAll();
        assertEquals(3, loaded.size());
        assertEquals("first",  loaded.get(0).getId());
        assertEquals("second", loaded.get(1).getId());
        assertEquals("third",  loaded.get(2).getId());
    }

    @Test
    void multipleProtocolsMixedRegistry() {
        registry.save(IdPConfig.builder().id("saml-idp").protocol(IdPConfig.Protocol.SAML).enabled(true).build());
        registry.save(IdPConfig.builder().id("oidc-idp").protocol(IdPConfig.Protocol.OIDC).enabled(true).build());

        assertEquals(2, registry.loadEnabled().size());
        assertEquals(IdPConfig.Protocol.SAML, registry.findById("saml-idp").getProtocol());
        assertEquals(IdPConfig.Protocol.OIDC, registry.findById("oidc-idp").getProtocol());
    }
}
