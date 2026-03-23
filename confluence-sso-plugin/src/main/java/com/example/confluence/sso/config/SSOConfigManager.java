package com.example.confluence.sso.config;

import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Persists and loads the global {@link SSOConfig} via Atlassian's PluginSettings API.
 *
 * Per-IdP settings are managed separately by {@link IdPRegistry}.
 */
@ConfluenceComponent
public class SSOConfigManager {

    private static final Logger log = LoggerFactory.getLogger(SSOConfigManager.class);
    private static final String PREFIX = "com.example.confluence.sso.global.";

    private final PluginSettings settings;

    @Autowired
    public SSOConfigManager(PluginSettingsFactory factory) {
        this.settings = factory.createGlobalSettings();
    }

    public SSOConfig load() {
        return SSOConfig.builder()
            .enabled(bool("enabled", false))
            .forceSSO(bool("forceSSO", false))
            .defaultRedirectPath(str("defaultRedirectPath", "/"))
            .autoProvisionUsers(bool("autoProvision", true))
            .syncGroupMembership(bool("syncGroups", false))
            .defaultGroup(str("defaultGroup", "confluence-users"))
            .build();
    }

    public void save(SSOConfig cfg) {
        put("enabled",             String.valueOf(cfg.isEnabled()));
        put("forceSSO",            String.valueOf(cfg.isForceSSO()));
        put("defaultRedirectPath", cfg.getDefaultRedirectPath());
        put("autoProvision",       String.valueOf(cfg.isAutoProvisionUsers()));
        put("syncGroups",          String.valueOf(cfg.isSyncGroupMembership()));
        put("defaultGroup",        cfg.getDefaultGroup());
        log.info("Global SSO config saved (enabled={}, forceSSO={})", cfg.isEnabled(), cfg.isForceSSO());
    }

    private String  str(String k, String def)    { Object v = settings.get(PREFIX + k); return v instanceof String && !((String)v).isBlank() ? (String)v : def; }
    private boolean bool(String k, boolean def)  { Object v = settings.get(PREFIX + k); return v instanceof String ? Boolean.parseBoolean((String)v) : def; }
    private void    put(String k, String v)      { settings.put(PREFIX + k, v != null ? v : ""); }
}
