package com.example.confluence.sso.servlet;

import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.user.User;
import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin UI for configuring the SSO plugin.
 *
 * GET  — renders the configuration form
 * POST — saves the submitted configuration
 *
 * URL: {@code /plugins/servlet/sso/admin}
 * Access is restricted to Confluence system administrators.
 */
public class SSOAdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SSOAdminServlet.class);
    private static final String TEMPLATE = "templates/admin.vm";

    private final SSOConfigManager   configManager;
    private final TemplateRenderer   renderer;
    private final PermissionManager  permissionManager;

    public SSOAdminServlet(SSOConfigManager configManager,
                           TemplateRenderer renderer,
                           PermissionManager permissionManager) {
        this.configManager    = configManager;
        this.renderer         = renderer;
        this.permissionManager = permissionManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!isAdmin(res)) return;

        SSOConfig config = configManager.load();
        Map<String, Object> ctx = buildTemplateContext(config, null);

        res.setContentType("text/html;charset=UTF-8");
        renderer.render(TEMPLATE, ctx, res.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!isAdmin(res)) return;

        String message;
        try {
            SSOConfig config = buildConfigFromRequest(req);
            configManager.save(config);
            message = "Configuration saved successfully.";
            log.info("SSO configuration updated by admin.");
        } catch (Exception e) {
            log.error("Failed to save SSO configuration", e);
            message = "Error saving configuration: " + e.getMessage();
        }

        SSOConfig updated = configManager.load();
        Map<String, Object> ctx = buildTemplateContext(updated, message);

        res.setContentType("text/html;charset=UTF-8");
        renderer.render(TEMPLATE, ctx, res.getWriter());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isAdmin(HttpServletResponse res) throws IOException {
        User currentUser = AuthenticatedUserThreadLocal.get();
        if (currentUser == null ||
            !permissionManager.hasPermission(currentUser, Permission.ADMINISTER, null)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Only Confluence system administrators can access SSO configuration.");
            return false;
        }
        return true;
    }

    private Map<String, Object> buildTemplateContext(SSOConfig config, String message) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("config",  config);
        ctx.put("message", message);
        ctx.put("baseUrl", "/plugins/servlet/sso/admin");
        return ctx;
    }

    private SSOConfig buildConfigFromRequest(HttpServletRequest req) {
        return SSOConfig.builder()
            .protocol(SSOConfig.Protocol.valueOf(param(req, "protocol", "SAML")))
            .enabled(Boolean.parseBoolean(param(req, "enabled", "false")))
            .forceSSO(Boolean.parseBoolean(param(req, "forceSSO", "false")))
            .defaultRedirectPath(param(req, "defaultRedirectPath", "/"))

            // SAML
            .samlIdpEntityId(param(req, "samlIdpEntityId", ""))
            .samlIdpSsoUrl(param(req, "samlIdpSsoUrl", ""))
            .samlIdpSloUrl(param(req, "samlIdpSloUrl", ""))
            .samlIdpCertificate(param(req, "samlIdpCertificate", ""))
            .samlSpEntityId(param(req, "samlSpEntityId", ""))
            .samlSpAcsUrl(param(req, "samlSpAcsUrl", ""))
            .samlSpSloUrl(param(req, "samlSpSloUrl", ""))
            .samlSpPrivateKey(param(req, "samlSpPrivateKey", ""))
            .samlSpCertificate(param(req, "samlSpCertificate", ""))
            .samlAttributeEmail(param(req, "samlAttributeEmail", "email"))
            .samlAttributeFullName(param(req, "samlAttributeFullName", "displayName"))
            .samlAttributeGroups(param(req, "samlAttributeGroups", "groups"))
            .samlSignRequests(Boolean.parseBoolean(param(req, "samlSignRequests", "true")))
            .samlWantAssertionsSigned(Boolean.parseBoolean(param(req, "samlWantAssertionsSigned", "true")))

            // OIDC
            .oidcIssuerUrl(param(req, "oidcIssuerUrl", ""))
            .oidcClientId(param(req, "oidcClientId", ""))
            .oidcClientSecret(param(req, "oidcClientSecret", ""))
            .oidcRedirectUri(param(req, "oidcRedirectUri", ""))
            .oidcScopes(param(req, "oidcScopes", "openid profile email"))
            .oidcClaimEmail(param(req, "oidcClaimEmail", "email"))
            .oidcClaimFullName(param(req, "oidcClaimFullName", "name"))
            .oidcClaimGroups(param(req, "oidcClaimGroups", "groups"))
            .oidcJwksUri(param(req, "oidcJwksUri", ""))
            .oidcPkceEnabled(Boolean.parseBoolean(param(req, "oidcPkceEnabled", "true")))

            // Provisioning
            .autoProvisionUsers(Boolean.parseBoolean(param(req, "autoProvisionUsers", "true")))
            .syncGroupMembership(Boolean.parseBoolean(param(req, "syncGroupMembership", "false")))
            .defaultGroup(param(req, "defaultGroup", "confluence-users"))
            .build();
    }

    private String param(HttpServletRequest req, String name, String defaultValue) {
        String v = req.getParameter(name);
        return (v != null && !v.isBlank()) ? v.trim() : defaultValue;
    }
}
