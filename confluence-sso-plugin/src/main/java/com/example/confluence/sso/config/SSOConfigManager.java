package com.example.confluence.sso.config;

import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Persists and loads {@link SSOConfig} via Atlassian's PluginSettings API,
 * which stores data in the Confluence database (band-aid storage).
 *
 * All keys are namespaced under {@value #KEY_PREFIX}.
 */
@ConfluenceComponent
public class SSOConfigManager {

    private static final Logger log = LoggerFactory.getLogger(SSOConfigManager.class);
    private static final String KEY_PREFIX = "com.example.confluence.sso.";

    private final PluginSettings settings;

    @Autowired
    public SSOConfigManager(PluginSettingsFactory factory) {
        this.settings = factory.createGlobalSettings();
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    public SSOConfig load() {
        return SSOConfig.builder()
            .protocol(parseProtocol(get("protocol", "SAML")))
            .enabled(Boolean.parseBoolean(get("enabled", "false")))
            .forceSSO(Boolean.parseBoolean(get("forceSSO", "false")))
            .defaultRedirectPath(get("defaultRedirectPath", "/"))

            .samlIdpEntityId(get("saml.idp.entityId", ""))
            .samlIdpSsoUrl(get("saml.idp.ssoUrl", ""))
            .samlIdpSloUrl(get("saml.idp.sloUrl", ""))
            .samlIdpCertificate(get("saml.idp.certificate", ""))
            .samlSpEntityId(get("saml.sp.entityId", ""))
            .samlSpAcsUrl(get("saml.sp.acsUrl", ""))
            .samlSpSloUrl(get("saml.sp.sloUrl", ""))
            .samlSpPrivateKey(get("saml.sp.privateKey", ""))
            .samlSpCertificate(get("saml.sp.certificate", ""))
            .samlAttributeEmail(get("saml.attr.email", "email"))
            .samlAttributeFullName(get("saml.attr.fullName", "displayName"))
            .samlAttributeGroups(get("saml.attr.groups", "groups"))
            .samlSignRequests(Boolean.parseBoolean(get("saml.signRequests", "true")))
            .samlWantAssertionsSigned(Boolean.parseBoolean(get("saml.wantAssertionsSigned", "true")))

            .oidcIssuerUrl(get("oidc.issuerUrl", ""))
            .oidcClientId(get("oidc.clientId", ""))
            .oidcClientSecret(get("oidc.clientSecret", ""))
            .oidcRedirectUri(get("oidc.redirectUri", ""))
            .oidcScopes(get("oidc.scopes", "openid profile email"))
            .oidcClaimEmail(get("oidc.claim.email", "email"))
            .oidcClaimFullName(get("oidc.claim.fullName", "name"))
            .oidcClaimGroups(get("oidc.claim.groups", "groups"))
            .oidcJwksUri(get("oidc.jwksUri", ""))
            .oidcPkceEnabled(Boolean.parseBoolean(get("oidc.pkceEnabled", "true")))

            .autoProvisionUsers(Boolean.parseBoolean(get("provision.autoProvision", "true")))
            .syncGroupMembership(Boolean.parseBoolean(get("provision.syncGroups", "false")))
            .defaultGroup(get("provision.defaultGroup", "confluence-users"))
            .build();
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public void save(SSOConfig cfg) {
        put("protocol",            cfg.getProtocol().name());
        put("enabled",             String.valueOf(cfg.isEnabled()));
        put("forceSSO",            String.valueOf(cfg.isForceSSO()));
        put("defaultRedirectPath", cfg.getDefaultRedirectPath());

        put("saml.idp.entityId",         cfg.getSamlIdpEntityId());
        put("saml.idp.ssoUrl",           cfg.getSamlIdpSsoUrl());
        put("saml.idp.sloUrl",           cfg.getSamlIdpSloUrl());
        put("saml.idp.certificate",      cfg.getSamlIdpCertificate());
        put("saml.sp.entityId",          cfg.getSamlSpEntityId());
        put("saml.sp.acsUrl",            cfg.getSamlSpAcsUrl());
        put("saml.sp.sloUrl",            cfg.getSamlSpSloUrl());
        put("saml.sp.privateKey",        cfg.getSamlSpPrivateKey());
        put("saml.sp.certificate",       cfg.getSamlSpCertificate());
        put("saml.attr.email",           cfg.getSamlAttributeEmail());
        put("saml.attr.fullName",        cfg.getSamlAttributeFullName());
        put("saml.attr.groups",          cfg.getSamlAttributeGroups());
        put("saml.signRequests",         String.valueOf(cfg.isSamlSignRequests()));
        put("saml.wantAssertionsSigned", String.valueOf(cfg.isSamlWantAssertionsSigned()));

        put("oidc.issuerUrl",      cfg.getOidcIssuerUrl());
        put("oidc.clientId",       cfg.getOidcClientId());
        put("oidc.clientSecret",   cfg.getOidcClientSecret());
        put("oidc.redirectUri",    cfg.getOidcRedirectUri());
        put("oidc.scopes",         cfg.getOidcScopes());
        put("oidc.claim.email",    cfg.getOidcClaimEmail());
        put("oidc.claim.fullName", cfg.getOidcClaimFullName());
        put("oidc.claim.groups",   cfg.getOidcClaimGroups());
        put("oidc.jwksUri",        cfg.getOidcJwksUri());
        put("oidc.pkceEnabled",    String.valueOf(cfg.isOidcPkceEnabled()));

        put("provision.autoProvision", String.valueOf(cfg.isAutoProvisionUsers()));
        put("provision.syncGroups",    String.valueOf(cfg.isSyncGroupMembership()));
        put("provision.defaultGroup",  cfg.getDefaultGroup());

        log.info("SSO configuration saved (protocol={}, enabled={})",
            cfg.getProtocol(), cfg.isEnabled());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String get(String key, String defaultValue) {
        Object v = settings.get(KEY_PREFIX + key);
        return (v instanceof String) ? (String) v : defaultValue;
    }

    private void put(String key, String value) {
        settings.put(KEY_PREFIX + key, value != null ? value : "");
    }

    private SSOConfig.Protocol parseProtocol(String value) {
        try {
            return SSOConfig.Protocol.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SSOConfig.Protocol.SAML;
        }
    }
}
