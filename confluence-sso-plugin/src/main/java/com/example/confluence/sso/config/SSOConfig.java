package com.example.confluence.sso.config;

/**
 * Immutable snapshot of the SSO plugin configuration.
 * Built via {@link SSOConfig.Builder} and persisted by {@link SSOConfigManager}.
 */
public final class SSOConfig {

    // ── General ──────────────────────────────────────────────────────────────
    public enum Protocol { SAML, OIDC }

    private final Protocol protocol;
    private final boolean  enabled;
    private final boolean  forceSSO;            // redirect all logins through SSO
    private final String   defaultRedirectPath; // where to land after login

    // ── SAML ─────────────────────────────────────────────────────────────────
    private final String samlIdpEntityId;
    private final String samlIdpSsoUrl;
    private final String samlIdpSloUrl;
    private final String samlIdpCertificate;    // PEM, base64-encoded
    private final String samlSpEntityId;
    private final String samlSpAcsUrl;
    private final String samlSpSloUrl;
    private final String samlSpPrivateKey;      // PEM, base64-encoded
    private final String samlSpCertificate;     // PEM, base64-encoded
    private final String samlAttributeEmail;
    private final String samlAttributeFullName;
    private final String samlAttributeGroups;
    private final boolean samlSignRequests;
    private final boolean samlWantAssertionsSigned;

    // ── OIDC ─────────────────────────────────────────────────────────────────
    private final String oidcIssuerUrl;
    private final String oidcClientId;
    private final String oidcClientSecret;
    private final String oidcRedirectUri;
    private final String oidcScopes;            // space-separated
    private final String oidcClaimEmail;
    private final String oidcClaimFullName;
    private final String oidcClaimGroups;
    private final String oidcJwksUri;           // optional override
    private final boolean oidcPkceEnabled;

    // ── User provisioning ─────────────────────────────────────────────────────
    private final boolean autoProvisionUsers;
    private final boolean syncGroupMembership;
    private final String  defaultGroup;

    private SSOConfig(Builder b) {
        this.protocol             = b.protocol;
        this.enabled              = b.enabled;
        this.forceSSO             = b.forceSSO;
        this.defaultRedirectPath  = b.defaultRedirectPath;

        this.samlIdpEntityId           = b.samlIdpEntityId;
        this.samlIdpSsoUrl             = b.samlIdpSsoUrl;
        this.samlIdpSloUrl             = b.samlIdpSloUrl;
        this.samlIdpCertificate        = b.samlIdpCertificate;
        this.samlSpEntityId            = b.samlSpEntityId;
        this.samlSpAcsUrl              = b.samlSpAcsUrl;
        this.samlSpSloUrl              = b.samlSpSloUrl;
        this.samlSpPrivateKey          = b.samlSpPrivateKey;
        this.samlSpCertificate         = b.samlSpCertificate;
        this.samlAttributeEmail        = b.samlAttributeEmail;
        this.samlAttributeFullName     = b.samlAttributeFullName;
        this.samlAttributeGroups       = b.samlAttributeGroups;
        this.samlSignRequests          = b.samlSignRequests;
        this.samlWantAssertionsSigned  = b.samlWantAssertionsSigned;

        this.oidcIssuerUrl     = b.oidcIssuerUrl;
        this.oidcClientId      = b.oidcClientId;
        this.oidcClientSecret  = b.oidcClientSecret;
        this.oidcRedirectUri   = b.oidcRedirectUri;
        this.oidcScopes        = b.oidcScopes;
        this.oidcClaimEmail    = b.oidcClaimEmail;
        this.oidcClaimFullName = b.oidcClaimFullName;
        this.oidcClaimGroups   = b.oidcClaimGroups;
        this.oidcJwksUri       = b.oidcJwksUri;
        this.oidcPkceEnabled   = b.oidcPkceEnabled;

        this.autoProvisionUsers  = b.autoProvisionUsers;
        this.syncGroupMembership = b.syncGroupMembership;
        this.defaultGroup        = b.defaultGroup;
    }

    public Protocol getProtocol()            { return protocol; }
    public boolean  isEnabled()              { return enabled; }
    public boolean  isForceSSO()             { return forceSSO; }
    public String   getDefaultRedirectPath() { return defaultRedirectPath; }

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

    public boolean isAutoProvisionUsers()  { return autoProvisionUsers; }
    public boolean isSyncGroupMembership() { return syncGroupMembership; }
    public String  getDefaultGroup()       { return defaultGroup; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Protocol protocol             = Protocol.SAML;
        private boolean  enabled              = false;
        private boolean  forceSSO             = false;
        private String   defaultRedirectPath  = "/";

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

        private boolean autoProvisionUsers  = true;
        private boolean syncGroupMembership = false;
        private String  defaultGroup        = "confluence-users";

        public Builder protocol(Protocol v)            { this.protocol = v;            return this; }
        public Builder enabled(boolean v)              { this.enabled = v;             return this; }
        public Builder forceSSO(boolean v)             { this.forceSSO = v;            return this; }
        public Builder defaultRedirectPath(String v)   { this.defaultRedirectPath = v; return this; }

        public Builder samlIdpEntityId(String v)         { this.samlIdpEntityId = v;          return this; }
        public Builder samlIdpSsoUrl(String v)           { this.samlIdpSsoUrl = v;            return this; }
        public Builder samlIdpSloUrl(String v)           { this.samlIdpSloUrl = v;            return this; }
        public Builder samlIdpCertificate(String v)      { this.samlIdpCertificate = v;       return this; }
        public Builder samlSpEntityId(String v)          { this.samlSpEntityId = v;           return this; }
        public Builder samlSpAcsUrl(String v)            { this.samlSpAcsUrl = v;             return this; }
        public Builder samlSpSloUrl(String v)            { this.samlSpSloUrl = v;             return this; }
        public Builder samlSpPrivateKey(String v)        { this.samlSpPrivateKey = v;         return this; }
        public Builder samlSpCertificate(String v)       { this.samlSpCertificate = v;        return this; }
        public Builder samlAttributeEmail(String v)      { this.samlAttributeEmail = v;       return this; }
        public Builder samlAttributeFullName(String v)   { this.samlAttributeFullName = v;    return this; }
        public Builder samlAttributeGroups(String v)     { this.samlAttributeGroups = v;      return this; }
        public Builder samlSignRequests(boolean v)       { this.samlSignRequests = v;         return this; }
        public Builder samlWantAssertionsSigned(boolean v){ this.samlWantAssertionsSigned = v; return this; }

        public Builder oidcIssuerUrl(String v)    { this.oidcIssuerUrl = v;    return this; }
        public Builder oidcClientId(String v)     { this.oidcClientId = v;     return this; }
        public Builder oidcClientSecret(String v) { this.oidcClientSecret = v; return this; }
        public Builder oidcRedirectUri(String v)  { this.oidcRedirectUri = v;  return this; }
        public Builder oidcScopes(String v)       { this.oidcScopes = v;       return this; }
        public Builder oidcClaimEmail(String v)   { this.oidcClaimEmail = v;   return this; }
        public Builder oidcClaimFullName(String v){ this.oidcClaimFullName = v; return this; }
        public Builder oidcClaimGroups(String v)  { this.oidcClaimGroups = v;  return this; }
        public Builder oidcJwksUri(String v)      { this.oidcJwksUri = v;      return this; }
        public Builder oidcPkceEnabled(boolean v) { this.oidcPkceEnabled = v;  return this; }

        public Builder autoProvisionUsers(boolean v)  { this.autoProvisionUsers = v;  return this; }
        public Builder syncGroupMembership(boolean v) { this.syncGroupMembership = v; return this; }
        public Builder defaultGroup(String v)         { this.defaultGroup = v;        return this; }

        public SSOConfig build() { return new SSOConfig(this); }
    }
}
