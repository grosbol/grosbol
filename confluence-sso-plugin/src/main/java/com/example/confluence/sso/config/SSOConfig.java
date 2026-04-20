package com.example.confluence.sso.config;

/**
 * Global SSO plugin settings (not per-IdP).
 *
 * Per-IdP settings live in {@link IdPConfig} objects managed by {@link IdPRegistry}.
 * This class only holds things that apply across all IdPs.
 */
public final class SSOConfig {

    private final boolean enabled;
    private final boolean forceSSO;
    private final String  defaultRedirectPath;
    private final boolean autoProvisionUsers;
    private final boolean syncGroupMembership;
    private final String  defaultGroup;

    private SSOConfig(Builder b) {
        this.enabled             = b.enabled;
        this.forceSSO            = b.forceSSO;
        this.defaultRedirectPath = b.defaultRedirectPath;
        this.autoProvisionUsers  = b.autoProvisionUsers;
        this.syncGroupMembership = b.syncGroupMembership;
        this.defaultGroup        = b.defaultGroup;
    }

    public boolean isEnabled()              { return enabled; }
    public boolean isForceSSO()             { return forceSSO; }
    public String  getDefaultRedirectPath() { return defaultRedirectPath; }
    public boolean isAutoProvisionUsers()   { return autoProvisionUsers; }
    public boolean isSyncGroupMembership()  { return syncGroupMembership; }
    public String  getDefaultGroup()        { return defaultGroup; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private boolean enabled             = false;
        private boolean forceSSO            = false;
        private String  defaultRedirectPath = "/";
        private boolean autoProvisionUsers  = true;
        private boolean syncGroupMembership = false;
        private String  defaultGroup        = "confluence-users";

        public Builder enabled(boolean v)             { this.enabled = v;             return this; }
        public Builder forceSSO(boolean v)            { this.forceSSO = v;            return this; }
        public Builder defaultRedirectPath(String v)  { this.defaultRedirectPath = v; return this; }
        public Builder autoProvisionUsers(boolean v)  { this.autoProvisionUsers = v;  return this; }
        public Builder syncGroupMembership(boolean v) { this.syncGroupMembership = v; return this; }
        public Builder defaultGroup(String v)         { this.defaultGroup = v;        return this; }

        public SSOConfig build() { return new SSOConfig(this); }
    }
}
