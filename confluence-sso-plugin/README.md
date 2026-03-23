# Confluence SSO Plugin

A production-ready Single Sign-On plugin for **Confluence 9.2** built with the Atlassian Plugin SDK.

Supports both **SAML 2.0** and **OpenID Connect / OAuth 2.0** protocols via Atlassian's Seraph authentication framework.

---

## Features

| Feature | Details |
|---------|---------|
| SAML 2.0 | HTTP-Redirect AuthnRequest, HTTP-POST ACS, SLO, signed assertions |
| OpenID Connect | PKCE (S256), auto-discovery, UserInfo endpoint |
| User provisioning | Auto-create accounts, sync group membership |
| Admin UI | Config form in Confluence's System Administration |
| SP Metadata | Auto-generated XML for IdP registration |
| Data Center | Compatible with Confluence Data Center clustering |

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Atlassian Plugin SDK | 8.x |
| Java | 11+ |
| Maven | 3.8+ |
| Confluence | 9.2.x |

Install the SDK:
```bash
# macOS
brew install atlassian-plugin-sdk

# Linux / Windows — download from:
# https://developer.atlassian.com/server/framework/atlassian-sdk/downloads/
```

---

## Build

```bash
cd confluence-sso-plugin

# Build the plugin JAR/OBR
atlas-mvn package

# Run Confluence locally for development (starts on port 1990)
atlas-run

# Quick-reload during development
atlas-mvn confluence:run
```

The built artifact will be at:
```
target/confluence-sso-plugin-1.0.0.jar
```

---

## Installation

1. Build the plugin (see above) or download from releases.
2. In Confluence: **Administration → Manage Apps → Upload App**.
3. Upload `confluence-sso-plugin-1.0.0.jar`.
4. Navigate to **Administration → SSO Configuration** to configure.

---

## Configuration

### Admin UI

Go to **Confluence Administration → SSO Configuration**
(`/plugins/servlet/sso/admin`)

### SAML 2.0 Setup

| Field | Description |
|-------|-------------|
| IdP Entity ID | Entity ID of your Identity Provider |
| IdP SSO URL | SSO endpoint URL of the IdP |
| IdP SLO URL | Single Logout endpoint (optional) |
| IdP Certificate | X.509 PEM certificate for verifying IdP signatures |
| SP Entity ID | Usually your Confluence base URL |
| ACS URL | `https://<your-confluence>/plugins/servlet/sso/saml/acs` |
| SP Private Key | PEM private key for signing AuthnRequests |
| SP Certificate | PEM certificate corresponding to the private key |

**Register the SP with your IdP** by downloading the metadata XML:
```
GET /plugins/servlet/sso/saml/metadata
```

### OIDC Setup

| Field | Description |
|-------|-------------|
| Issuer URL | e.g. `https://accounts.google.com` or `https://tenant.okta.com` |
| Client ID | From your IdP application |
| Client Secret | From your IdP application |
| Redirect URI | `https://<your-confluence>/plugins/servlet/sso/oidc/callback` |
| Scopes | `openid profile email` (default) |
| PKCE | Enabled by default (S256) |

### User Provisioning

| Setting | Default | Description |
|---------|---------|-------------|
| Auto-provision users | ON | Create Confluence accounts for new SSO users |
| Sync group membership | OFF | Add users to groups matching IdP group claims |
| Default group | `confluence-users` | Added to every SSO-authenticated user |

---

## Endpoints

| URL | Description |
|-----|-------------|
| `GET /plugins/servlet/sso/saml/login` | Initiate SAML SSO |
| `POST /plugins/servlet/sso/saml/acs` | SAML Assertion Consumer Service |
| `GET /plugins/servlet/sso/saml/metadata` | SP metadata XML |
| `GET /plugins/servlet/sso/saml/slo` | SAML Single Logout |
| `GET /plugins/servlet/sso/oidc/login` | Initiate OIDC flow |
| `GET /plugins/servlet/sso/oidc/callback` | OIDC callback |
| `GET /plugins/servlet/sso/logout` | Protocol-agnostic logout |
| `GET /plugins/servlet/sso/admin` | Admin configuration UI |

---

## Architecture

```
confluence-sso-plugin/
├── pom.xml                             Maven / AMPS build descriptor
└── src/main/
    ├── resources/
    │   ├── atlassian-plugin.xml        Plugin descriptor
    │   ├── templates/admin.vm          Velocity admin UI template
    │   ├── css/sso-admin.css
    │   ├── js/sso-admin.js
    │   └── i18n/sso-plugin.properties
    └── java/com/example/confluence/sso/
        ├── auth/
        │   ├── SSOFilter.java          Servlet filter — enforces SSO on every request
        │   └── SSOAuthenticator.java   Seraph authenticator — bridges SSO session → Confluence
        ├── saml/
        │   ├── SAMLHandler.java        AuthnRequest builder + Response validator (OpenSAML 4)
        │   ├── SAMLUserInfo.java       Parsed assertion value object
        │   ├── SAMLEncodingUtil.java   Redirect-binding encode/decode helpers
        │   ├── SAMLKeyUtil.java        KeyDescriptor builder
        │   └── SAMLException.java
        ├── oidc/
        │   ├── OIDCHandler.java        Authorization URL builder + token exchange (Nimbus SDK)
        │   ├── OIDCUserInfo.java       Parsed UserInfo value object
        │   └── OIDCException.java
        ├── config/
        │   ├── SSOConfig.java          Immutable config value object + Builder
        │   └── SSOConfigManager.java   Persist/load config via SAL PluginSettings
        ├── servlet/
        │   ├── SAMLLoginServlet.java   GET → builds AuthnRequest + redirects to IdP
        │   ├── SAMLACSServlet.java     POST ← validates IdP response, establishes session
        │   ├── SAMLMetadataServlet.java GET → serves SP metadata XML
        │   ├── SAMLSLOServlet.java     GET → initiates SAML SLO
        │   ├── OIDCLoginServlet.java   GET → builds authorization URL + redirects
        │   ├── OIDCCallbackServlet.java GET ← exchanges code, establishes session
        │   ├── SSOLogoutServlet.java   GET → protocol-agnostic logout
        │   └── SSOAdminServlet.java    GET/POST → admin configuration UI
        └── util/
            ├── SSOSessionUtil.java         Session attribute helpers
            └── UserProvisioningService.java Find/create/update Confluence users
```

---

## Seraph Integration

To use the custom authenticator, add this to Confluence's `seraph-config.xml`
(found at `<confluence-home>/confluence/WEB-INF/classes/seraph-config.xml`):

```xml
<authenticator class="com.example.confluence.sso.auth.SSOAuthenticator"/>
```

For most deployments the **servlet filter alone is sufficient** — it intercepts
unauthenticated requests and completes the SSO flow, after which Confluence's
default session handling takes over.

---

## Running Tests

```bash
atlas-mvn test
```

---

## Security Notes

- All SAML assertions are signature-validated before use.
- OIDC flows use PKCE (S256) by default to prevent authorization code interception.
- Relay state (SAML) and OAuth state (OIDC) are validated to prevent CSRF.
- Private keys and client secrets are stored in Confluence's encrypted PluginSettings store.
- The admin page is restricted to Confluence System Administrators.
