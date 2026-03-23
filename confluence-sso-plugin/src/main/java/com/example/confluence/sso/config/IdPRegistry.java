package com.example.confluence.sso.config;

import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Stores and retrieves the ordered list of {@link IdPConfig} objects.
 *
 * <p>Storage layout in PluginSettings:
 * <pre>
 * sso.idp.ids                  → comma-separated ordered list of IdP IDs
 * sso.idp.{id}.displayName     → per-IdP field
 * sso.idp.{id}.protocol        → ...
 * sso.idp.{id}.*               → all other fields mirror IdPConfig properties
 * </pre>
 */
@ConfluenceComponent
public class IdPRegistry {

    private static final Logger log = LoggerFactory.getLogger(IdPRegistry.class);
    static final String PREFIX   = "com.example.confluence.sso.idp.";
    static final String IDS_KEY  = PREFIX + "ids";

    private final PluginSettings settings;

    @Autowired
    public IdPRegistry(PluginSettingsFactory factory) {
        this.settings = factory.createGlobalSettings();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Returns all IdPs in the order they were saved (enabled and disabled). */
    public List<IdPConfig> loadAll() {
        List<String> ids = loadIds();
        List<IdPConfig> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            IdPConfig cfg = loadOne(id);
            if (cfg != null) result.add(cfg);
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns only the enabled IdPs. */
    public List<IdPConfig> loadEnabled() {
        List<IdPConfig> all = loadAll();
        List<IdPConfig> enabled = new ArrayList<>();
        for (IdPConfig c : all) {
            if (c.isEnabled()) enabled.add(c);
        }
        return Collections.unmodifiableList(enabled);
    }

    /** Returns the IdP with the given id, or {@code null} if not found. */
    public IdPConfig findById(String id) {
        if (id == null || id.isBlank()) return null;
        return loadOne(id);
    }

    /**
     * Find the IdP to use for a given email address using domain-hint matching.
     * If multiple IdPs match the domain the first one wins.
     * Returns {@code null} if no domain match found.
     */
    public IdPConfig findByEmailDomain(String email) {
        if (email == null || !email.contains("@")) return null;
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();
        for (IdPConfig idp : loadEnabled()) {
            if (idp.matchesDomain(domain)) return idp;
        }
        return null;
    }

    /**
     * Returns the single default IdP when only one enabled IdP is configured,
     * or {@code null} when there are zero or multiple enabled IdPs.
     */
    public IdPConfig defaultIfSingle() {
        List<IdPConfig> enabled = loadEnabled();
        return enabled.size() == 1 ? enabled.get(0) : null;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Persist or overwrite a single IdP config (order is preserved / appended). */
    public void save(IdPConfig idp) {
        List<String> ids = new ArrayList<>(loadIds());
        if (!ids.contains(idp.getId())) {
            ids.add(idp.getId());
            settings.put(IDS_KEY, String.join(",", ids));
        }
        saveFields(idp);
        log.info("Saved IdP '{}' ({})", idp.getId(), idp.getDisplayName());
    }

    /** Save every IdP in the list, replacing the current ordered set. */
    public void saveAll(List<IdPConfig> idps) {
        List<String> ids = new ArrayList<>();
        for (IdPConfig idp : idps) {
            ids.add(idp.getId());
            saveFields(idp);
        }
        settings.put(IDS_KEY, String.join(",", ids));
        log.info("Saved {} IdP(s): {}", ids.size(), ids);
    }

    /** Remove an IdP by id. */
    public void delete(String id) {
        List<String> ids = new ArrayList<>(loadIds());
        if (ids.remove(id)) {
            settings.put(IDS_KEY, String.join(",", ids));
            clearFields(id);
            log.info("Deleted IdP '{}'", id);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<String> loadIds() {
        Object raw = settings.get(IDS_KEY);
        if (!(raw instanceof String) || ((String) raw).isBlank()) return Collections.emptyList();
        List<String> ids = new ArrayList<>();
        for (String part : ((String) raw).split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) ids.add(trimmed);
        }
        return ids;
    }

    private IdPConfig loadOne(String id) {
        String p = PREFIX + id + ".";
        String displayName = get(p + "displayName");
        if (displayName == null) return null;   // IdP doesn't exist

        return IdPConfig.builder()
            .id(id)
            .displayName(displayName)
            .enabled(bool(get(p + "enabled"), true))
            .protocol(parseProtocol(get(p + "protocol")))
            .domainHint(str(get(p + "domainHint")))
            .samlIdpEntityId(str(get(p + "samlIdpEntityId")))
            .samlIdpSsoUrl(str(get(p + "samlIdpSsoUrl")))
            .samlIdpSloUrl(str(get(p + "samlIdpSloUrl")))
            .samlIdpCertificate(str(get(p + "samlIdpCertificate")))
            .samlSpEntityId(str(get(p + "samlSpEntityId")))
            .samlSpAcsUrl(str(get(p + "samlSpAcsUrl")))
            .samlSpSloUrl(str(get(p + "samlSpSloUrl")))
            .samlSpPrivateKey(str(get(p + "samlSpPrivateKey")))
            .samlSpCertificate(str(get(p + "samlSpCertificate")))
            .samlAttributeEmail(str(get(p + "samlAttributeEmail"), "email"))
            .samlAttributeFullName(str(get(p + "samlAttributeFullName"), "displayName"))
            .samlAttributeGroups(str(get(p + "samlAttributeGroups"), "groups"))
            .samlSignRequests(bool(get(p + "samlSignRequests"), true))
            .samlWantAssertionsSigned(bool(get(p + "samlWantAssertionsSigned"), true))
            .oidcIssuerUrl(str(get(p + "oidcIssuerUrl")))
            .oidcClientId(str(get(p + "oidcClientId")))
            .oidcClientSecret(str(get(p + "oidcClientSecret")))
            .oidcRedirectUri(str(get(p + "oidcRedirectUri")))
            .oidcScopes(str(get(p + "oidcScopes"), "openid profile email"))
            .oidcClaimEmail(str(get(p + "oidcClaimEmail"), "email"))
            .oidcClaimFullName(str(get(p + "oidcClaimFullName"), "name"))
            .oidcClaimGroups(str(get(p + "oidcClaimGroups"), "groups"))
            .oidcJwksUri(str(get(p + "oidcJwksUri")))
            .oidcPkceEnabled(bool(get(p + "oidcPkceEnabled"), true))
            .build();
    }

    private void saveFields(IdPConfig idp) {
        String p = PREFIX + idp.getId() + ".";
        put(p + "displayName",             idp.getDisplayName());
        put(p + "enabled",                 String.valueOf(idp.isEnabled()));
        put(p + "protocol",                idp.getProtocol().name());
        put(p + "domainHint",              idp.getDomainHint());
        put(p + "samlIdpEntityId",         idp.getSamlIdpEntityId());
        put(p + "samlIdpSsoUrl",           idp.getSamlIdpSsoUrl());
        put(p + "samlIdpSloUrl",           idp.getSamlIdpSloUrl());
        put(p + "samlIdpCertificate",      idp.getSamlIdpCertificate());
        put(p + "samlSpEntityId",          idp.getSamlSpEntityId());
        put(p + "samlSpAcsUrl",            idp.getSamlSpAcsUrl());
        put(p + "samlSpSloUrl",            idp.getSamlSpSloUrl());
        put(p + "samlSpPrivateKey",        idp.getSamlSpPrivateKey());
        put(p + "samlSpCertificate",       idp.getSamlSpCertificate());
        put(p + "samlAttributeEmail",      idp.getSamlAttributeEmail());
        put(p + "samlAttributeFullName",   idp.getSamlAttributeFullName());
        put(p + "samlAttributeGroups",     idp.getSamlAttributeGroups());
        put(p + "samlSignRequests",        String.valueOf(idp.isSamlSignRequests()));
        put(p + "samlWantAssertionsSigned",String.valueOf(idp.isSamlWantAssertionsSigned()));
        put(p + "oidcIssuerUrl",           idp.getOidcIssuerUrl());
        put(p + "oidcClientId",            idp.getOidcClientId());
        put(p + "oidcClientSecret",        idp.getOidcClientSecret());
        put(p + "oidcRedirectUri",         idp.getOidcRedirectUri());
        put(p + "oidcScopes",              idp.getOidcScopes());
        put(p + "oidcClaimEmail",          idp.getOidcClaimEmail());
        put(p + "oidcClaimFullName",       idp.getOidcClaimFullName());
        put(p + "oidcClaimGroups",         idp.getOidcClaimGroups());
        put(p + "oidcJwksUri",             idp.getOidcJwksUri());
        put(p + "oidcPkceEnabled",         String.valueOf(idp.isOidcPkceEnabled()));
    }

    private static final List<String> FIELD_SUFFIXES = List.of(
        "displayName","enabled","protocol","domainHint",
        "samlIdpEntityId","samlIdpSsoUrl","samlIdpSloUrl","samlIdpCertificate",
        "samlSpEntityId","samlSpAcsUrl","samlSpSloUrl","samlSpPrivateKey","samlSpCertificate",
        "samlAttributeEmail","samlAttributeFullName","samlAttributeGroups",
        "samlSignRequests","samlWantAssertionsSigned",
        "oidcIssuerUrl","oidcClientId","oidcClientSecret","oidcRedirectUri","oidcScopes",
        "oidcClaimEmail","oidcClaimFullName","oidcClaimGroups","oidcJwksUri","oidcPkceEnabled"
    );

    private void clearFields(String id) {
        String p = PREFIX + id + ".";
        for (String suffix : FIELD_SUFFIXES) {
            settings.remove(p + suffix);
        }
    }

    private String  get(String key)                        { Object v = settings.get(key); return v instanceof String ? (String)v : null; }
    private void    put(String key, String value)          { settings.put(key, value != null ? value : ""); }
    private String  str(String v)                          { return v != null ? v : ""; }
    private String  str(String v, String def)              { return (v != null && !v.isBlank()) ? v : def; }
    private boolean bool(String v, boolean def)            { return v != null ? Boolean.parseBoolean(v) : def; }
    private IdPConfig.Protocol parseProtocol(String v) {
        try { return IdPConfig.Protocol.valueOf(v.toUpperCase()); } catch (Exception e) { return IdPConfig.Protocol.SAML; }
    }
}
