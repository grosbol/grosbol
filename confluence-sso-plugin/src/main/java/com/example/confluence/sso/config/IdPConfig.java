package com.example.confluence.sso.config;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for a single Identity Provider.
 *
 * One {@link SSOConfig} holds a list of these. Each IdP has a unique {@code id}
 * (a short slug chosen by the admin, e.g. {@code "okta"} or {@code "azure"})
 * and an optional {@code domainHint} (comma-separated list of email domains that
 * should be automatically routed to this IdP, e.g. {@code "corp.com,corp.net"}).
 */
public final class IdPConfig {

    public enum Protocol { SAML, OIDC }

    // ── Identity ──────────────────────────────────────────────────────────────
    private final String   id;           // unique slug, used as URL/storage key
    private final String   displayName;  // shown on the IdP-picker page
    private final boolean  enabled;
    private final Protocol protocol;
    private final String   domainHint;   // comma-separated email domains → auto-route

    // ── SAML ──────────────────────────────────────────────────────────────────
    private final String  samlIdpEntityId;
    private final String  samlIdpSsoUrl;
    private final String  samlIdpSloUrl;
    private final String  samlIdpCertificate;
    private final String  samlSpEntityId;
    private final String  samlSpAcsUrl;
    private final String  samlSpSloUrl;
    private final String  samlSpPrivateKey;
    private final String  samlSpCertificate;
    private final String  samlAttributeEmail;
    private final String  samlAttributeFullName;
    private final String  samlAttributeGroups;
    private final boolean samlSignRequests;
    private final boolean samlWantAssertionsSigned;

    // ── OIDC ──────────────────────────────────────────────────────────────────
    private final String  oidcIssuerUrl;
    private final String  oidcClientId;
    private final String  oidcClientSecret;
    private final String  oidcRedirectUri;
    private final String  oidcScopes;
    private final String  oidcClaimEmail;
    private final String  oidcClaimFullName;
    private final String  oidcClaimGroups;
    private final String  oidcJwksUri;
    private final boolean oidcPkceEnabled;

    private IdPConfig(Builder b) {
        this.id                      = b.id;
        this.displayName             = b.displayName;
        this.enabled                 = b.enabled;
        this.protocol                = b.protocol;
        this.domainHint              = b.domainHint;
        this.samlIdpEntityId         = b.samlIdpEntityId;
        this.samlIdpSsoUrl           = b.samlIdpSsoUrl;
        this.samlIdpSloUrl           = b.samlIdpSloUrl;
        this.samlIdpCertificate      = b.samlIdpCertificate;
        this.samlSpEntityId          = b.samlSpEntityId;
        this.samlSpAcsUrl            = b.samlSpAcsUrl;
        this.samlSpSloUrl            = b.samlSpSloUrl;
        this.samlSpPrivateKey        = b.samlSpPrivateKey;
        this.samlSpCertificate       = b.samlSpCertificate;
        this.samlAttributeEmail      = b.samlAttributeEmail;
        this.samlAttributeFullName   = b.samlAttributeFullName;
        this.samlAttributeGroups     = b.samlAttributeGroups;
        this.samlSignRequests        = b.samlSignRequests;
        this.samlWantAssertionsSigned= b.samlWantAssertionsSigned;
        this.oidcIssuerUrl           = b.oidcIssuerUrl;
        this.oidcClientId            = b.oidcClientId;
        this.oidcClientSecret        = b.oidcClientSecret;
        this.oidcRedirectUri         = b.oidcRedirectUri;
        this.oidcScopes              = b.oidcScopes;
        this.oidcClaimEmail          = b.oidcClaimEmail;
        this.oidcClaimFullName       = b.oidcClaimFullName;
        this.oidcClaimGroups         = b.oidcClaimGroups;
        this.oidcJwksUri             = b.oidcJwksUri;
        this.oidcPkceEnabled         = b.oidcPkceEnabled;
    }

    public String   getId()                      { return id; }
    public String   getDisplayName()             { return displayName; }
    public boolean  isEnabled()                  { return enabled; }
    public Protocol getProtocol()                { return protocol; }
    public String   getDomainHint()              { return domainHint; }

    public String  getSamlIdpEntityId()          { return samlIdpEntityId; }
    public String  getSamlIdpSsoUrl()            { return samlIdpSsoUrl; }
    public String  getSamlIdpSloUrl()            { return samlIdpSloUrl; }
    public String  getSamlIdpCertificate()       { return samlIdpCertificate; }
    public String  getSamlSpEntityId()           { return samlSpEntityId; }
    public String  getSamlSpAcsUrl()             { return samlSpAcsUrl; }
    public String  getSamlSpSloUrl()             { return samlSpSloUrl; }
    public String  getSamlSpPrivateKey()         { return samlSpPrivateKey; }
    public String  getSamlSpCertificate()        { return samlSpCertificate; }
    public String  getSamlAttributeEmail()       { return samlAttributeEmail; }
    public String  getSamlAttributeFullName()    { return samlAttributeFullName; }
    public String  getSamlAttributeGroups()      { return samlAttributeGroups; }
    public boolean isSamlSignRequests()          { return samlSignRequests; }
    public boolean isSamlWantAssertionsSigned()  { return samlWantAssertionsSigned; }

    public String  getOidcIssuerUrl()    { return oidcIssuerUrl; }
    public String  getOidcClientId()     { return oidcClientId; }
    public String  getOidcClientSecret() { return oidcClientSecret; }
    public String  getOidcRedirectUri()  { return oidcRedirectUri; }
    public String  getOidcScopes()       { return oidcScopes; }
    public String  getOidcClaimEmail()   { return oidcClaimEmail; }
    public String  getOidcClaimFullName(){ return oidcClaimFullName; }
    public String  getOidcClaimGroups()  { return oidcClaimGroups; }
    public String  getOidcJwksUri()      { return oidcJwksUri; }
    public boolean isOidcPkceEnabled()   { return oidcPkceEnabled; }

    /** True if this IdP should handle the given email domain. */
    public boolean matchesDomain(String emailDomain) {
        if (domainHint == null || domainHint.isBlank()) return false;
        for (String d : domainHint.split(",")) {
            if (d.trim().equalsIgnoreCase(emailDomain)) return true;
        }
        return false;
    }

    public static Builder builder() { return new Builder(); }

    /** Return a copy of this config as a builder (for edit-and-save flows). */
    public Builder toBuilder() {
        return builder()
            .id(id).displayName(displayName).enabled(enabled).protocol(protocol).domainHint(domainHint)
            .samlIdpEntityId(samlIdpEntityId).samlIdpSsoUrl(samlIdpSsoUrl).samlIdpSloUrl(samlIdpSloUrl)
            .samlIdpCertificate(samlIdpCertificate).samlSpEntityId(samlSpEntityId)
            .samlSpAcsUrl(samlSpAcsUrl).samlSpSloUrl(samlSpSloUrl)
            .samlSpPrivateKey(samlSpPrivateKey).samlSpCertificate(samlSpCertificate)
            .samlAttributeEmail(samlAttributeEmail).samlAttributeFullName(samlAttributeFullName)
            .samlAttributeGroups(samlAttributeGroups).samlSignRequests(samlSignRequests)
            .samlWantAssertionsSigned(samlWantAssertionsSigned)
            .oidcIssuerUrl(oidcIssuerUrl).oidcClientId(oidcClientId).oidcClientSecret(oidcClientSecret)
            .oidcRedirectUri(oidcRedirectUri).oidcScopes(oidcScopes)
            .oidcClaimEmail(oidcClaimEmail).oidcClaimFullName(oidcClaimFullName).oidcClaimGroups(oidcClaimGroups)
            .oidcJwksUri(oidcJwksUri).oidcPkceEnabled(oidcPkceEnabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdPConfig)) return false;
        return Objects.equals(id, ((IdPConfig) o).id);
    }

    @Override public int hashCode() { return Objects.hash(id); }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static final class Builder {
        private String   id            = UUID.randomUUID().toString().replace("-","").substring(0,8);
        private String   displayName   = "Identity Provider";
        private boolean  enabled       = true;
        private Protocol protocol      = Protocol.SAML;
        private String   domainHint    = "";

        private String  samlIdpEntityId          = "";
        private String  samlIdpSsoUrl            = "";
        private String  samlIdpSloUrl            = "";
        private String  samlIdpCertificate       = "";
        private String  samlSpEntityId           = "";
        private String  samlSpAcsUrl             = "";
        private String  samlSpSloUrl             = "";
        private String  samlSpPrivateKey         = "";
        private String  samlSpCertificate        = "";
        private String  samlAttributeEmail       = "email";
        private String  samlAttributeFullName    = "displayName";
        private String  samlAttributeGroups      = "groups";
        private boolean samlSignRequests         = true;
        private boolean samlWantAssertionsSigned = true;

        private String  oidcIssuerUrl    = "";
        private String  oidcClientId     = "";
        private String  oidcClientSecret = "";
        private String  oidcRedirectUri  = "";
        private String  oidcScopes       = "openid profile email";
        private String  oidcClaimEmail   = "email";
        private String  oidcClaimFullName= "name";
        private String  oidcClaimGroups  = "groups";
        private String  oidcJwksUri      = "";
        private boolean oidcPkceEnabled  = true;

        public Builder id(String v)                        { this.id = v;                       return this; }
        public Builder displayName(String v)               { this.displayName = v;               return this; }
        public Builder enabled(boolean v)                  { this.enabled = v;                   return this; }
        public Builder protocol(Protocol v)                { this.protocol = v;                  return this; }
        public Builder domainHint(String v)                { this.domainHint = v;                return this; }
        public Builder samlIdpEntityId(String v)           { this.samlIdpEntityId = v;           return this; }
        public Builder samlIdpSsoUrl(String v)             { this.samlIdpSsoUrl = v;             return this; }
        public Builder samlIdpSloUrl(String v)             { this.samlIdpSloUrl = v;             return this; }
        public Builder samlIdpCertificate(String v)        { this.samlIdpCertificate = v;        return this; }
        public Builder samlSpEntityId(String v)            { this.samlSpEntityId = v;            return this; }
        public Builder samlSpAcsUrl(String v)              { this.samlSpAcsUrl = v;              return this; }
        public Builder samlSpSloUrl(String v)              { this.samlSpSloUrl = v;              return this; }
        public Builder samlSpPrivateKey(String v)          { this.samlSpPrivateKey = v;          return this; }
        public Builder samlSpCertificate(String v)         { this.samlSpCertificate = v;         return this; }
        public Builder samlAttributeEmail(String v)        { this.samlAttributeEmail = v;        return this; }
        public Builder samlAttributeFullName(String v)     { this.samlAttributeFullName = v;     return this; }
        public Builder samlAttributeGroups(String v)       { this.samlAttributeGroups = v;       return this; }
        public Builder samlSignRequests(boolean v)         { this.samlSignRequests = v;          return this; }
        public Builder samlWantAssertionsSigned(boolean v) { this.samlWantAssertionsSigned = v;  return this; }
        public Builder oidcIssuerUrl(String v)             { this.oidcIssuerUrl = v;             return this; }
        public Builder oidcClientId(String v)              { this.oidcClientId = v;              return this; }
        public Builder oidcClientSecret(String v)          { this.oidcClientSecret = v;          return this; }
        public Builder oidcRedirectUri(String v)           { this.oidcRedirectUri = v;           return this; }
        public Builder oidcScopes(String v)                { this.oidcScopes = v;                return this; }
        public Builder oidcClaimEmail(String v)            { this.oidcClaimEmail = v;            return this; }
        public Builder oidcClaimFullName(String v)         { this.oidcClaimFullName = v;         return this; }
        public Builder oidcClaimGroups(String v)           { this.oidcClaimGroups = v;           return this; }
        public Builder oidcJwksUri(String v)               { this.oidcJwksUri = v;              return this; }
        public Builder oidcPkceEnabled(boolean v)          { this.oidcPkceEnabled = v;           return this; }

        public IdPConfig build() { return new IdPConfig(this); }
    }
}
