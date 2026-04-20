package com.example.confluence.sso.servlet;

import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.user.User;
import com.example.confluence.sso.config.IdPConfig;
import com.example.confluence.sso.config.IdPRegistry;
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
 * Admin UI for the SSO plugin.
 *
 * Handles:
 * <ul>
 *   <li>GET  /admin             — list all IdPs + global settings</li>
 *   <li>POST /admin             — save global settings</li>
 *   <li>GET  /admin?action=edit&idp=&lt;id&gt;  — edit/create IdP form</li>
 *   <li>POST /admin?action=save-idp             — save one IdP</li>
 *   <li>POST /admin?action=delete-idp&idp=&lt;id&gt; — delete an IdP</li>
 * </ul>
 *
 * URL: {@code /plugins/servlet/sso/admin}
 */
public class SSOAdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SSOAdminServlet.class);

    private final SSOConfigManager  configManager;
    private final IdPRegistry       idpRegistry;
    private final TemplateRenderer  renderer;
    private final PermissionManager permissionManager;

    public SSOAdminServlet(SSOConfigManager configManager,
                           IdPRegistry idpRegistry,
                           TemplateRenderer renderer,
                           PermissionManager permissionManager) {
        this.configManager    = configManager;
        this.idpRegistry      = idpRegistry;
        this.renderer         = renderer;
        this.permissionManager = permissionManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!isAdmin(res)) return;

        String action = coalesce(req.getParameter("action"), "list");
        String idpId  = req.getParameter("idp");

        res.setContentType("text/html;charset=UTF-8");

        if ("edit".equals(action)) {
            // Edit existing or create new IdP form
            IdPConfig editing = idpId != null ? idpRegistry.findById(idpId) : null;
            if (editing == null) {
                editing = IdPConfig.builder().build();   // blank new IdP
            }
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("editing", editing);
            ctx.put("isNew",   idpId == null || idpRegistry.findById(idpId) == null);
            ctx.put("baseUrl", "/plugins/servlet/sso/admin");
            renderer.render("templates/admin-idp-edit.vm", ctx, res.getWriter());
        } else {
            // Main list view
            renderList(req, res, null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!isAdmin(res)) return;

        String action = coalesce(req.getParameter("action"), "save-global");
        String message;

        switch (action) {
            case "save-idp":
                message = saveIdP(req);
                break;
            case "delete-idp":
                message = deleteIdP(req);
                break;
            default:
                message = saveGlobal(req);
        }

        renderList(req, res, message);
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private String saveGlobal(HttpServletRequest req) {
        try {
            SSOConfig cfg = SSOConfig.builder()
                .enabled(bool(req, "enabled"))
                .forceSSO(bool(req, "forceSSO"))
                .defaultRedirectPath(param(req, "defaultRedirectPath", "/"))
                .autoProvisionUsers(bool(req, "autoProvisionUsers", true))
                .syncGroupMembership(bool(req, "syncGroupMembership"))
                .defaultGroup(param(req, "defaultGroup", "confluence-users"))
                .build();
            configManager.save(cfg);
            log.info("Global SSO settings updated by admin.");
            return "Global settings saved.";
        } catch (Exception e) {
            log.error("Failed to save global settings", e);
            return "Error: " + e.getMessage();
        }
    }

    private String saveIdP(HttpServletRequest req) {
        try {
            IdPConfig idp = IdPConfig.builder()
                .id(param(req, "id", ""))
                .displayName(param(req, "displayName", "Identity Provider"))
                .enabled(bool(req, "enabled", true))
                .protocol(parseProtocol(param(req, "protocol", "SAML")))
                .domainHint(param(req, "domainHint", ""))
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
                .samlSignRequests(bool(req, "samlSignRequests", true))
                .samlWantAssertionsSigned(bool(req, "samlWantAssertionsSigned", true))
                .oidcIssuerUrl(param(req, "oidcIssuerUrl", ""))
                .oidcClientId(param(req, "oidcClientId", ""))
                .oidcClientSecret(param(req, "oidcClientSecret", ""))
                .oidcRedirectUri(param(req, "oidcRedirectUri", ""))
                .oidcScopes(param(req, "oidcScopes", "openid profile email"))
                .oidcClaimEmail(param(req, "oidcClaimEmail", "email"))
                .oidcClaimFullName(param(req, "oidcClaimFullName", "name"))
                .oidcClaimGroups(param(req, "oidcClaimGroups", "groups"))
                .oidcJwksUri(param(req, "oidcJwksUri", ""))
                .oidcPkceEnabled(bool(req, "oidcPkceEnabled", true))
                .build();

            if (idp.getId().isBlank()) {
                return "Error: IdP ID is required.";
            }
            idpRegistry.save(idp);
            log.info("IdP '{}' saved by admin.", idp.getId());
            return "IdP '" + idp.getDisplayName() + "' saved.";
        } catch (Exception e) {
            log.error("Failed to save IdP", e);
            return "Error saving IdP: " + e.getMessage();
        }
    }

    private String deleteIdP(HttpServletRequest req) {
        String idpId = req.getParameter("idp");
        if (idpId == null || idpId.isBlank()) return "Error: no IdP id specified.";
        idpRegistry.delete(idpId);
        log.info("IdP '{}' deleted by admin.", idpId);
        return "IdP '" + idpId + "' deleted.";
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderList(HttpServletRequest req, HttpServletResponse res, String message)
            throws IOException {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("global",  configManager.load());
        ctx.put("idps",    idpRegistry.loadAll());
        ctx.put("message", message);
        ctx.put("baseUrl", "/plugins/servlet/sso/admin");
        res.setContentType("text/html;charset=UTF-8");
        renderer.render("templates/admin.vm", ctx, res.getWriter());
    }

    // ── Auth guard ────────────────────────────────────────────────────────────

    private boolean isAdmin(HttpServletResponse res) throws IOException {
        User user = AuthenticatedUserThreadLocal.get();
        if (user == null || !permissionManager.hasPermission(user, Permission.ADMINISTER, null)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Only Confluence system administrators can access SSO configuration.");
            return false;
        }
        return true;
    }

    // ── Parameter helpers ─────────────────────────────────────────────────────

    private String param(HttpServletRequest req, String name, String def) {
        String v = req.getParameter(name);
        return (v != null && !v.isBlank()) ? v.trim() : def;
    }
    private boolean bool(HttpServletRequest req, String name) {
        return bool(req, name, false);
    }
    private boolean bool(HttpServletRequest req, String name, boolean def) {
        String v = req.getParameter(name);
        return v != null ? Boolean.parseBoolean(v) : def;
    }
    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
    private IdPConfig.Protocol parseProtocol(String v) {
        try { return IdPConfig.Protocol.valueOf(v.toUpperCase()); } catch (Exception e) { return IdPConfig.Protocol.SAML; }
    }
}
