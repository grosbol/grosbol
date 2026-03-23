package com.example.confluence.sso.servlet;

import com.atlassian.templaterenderer.TemplateRenderer;
import com.example.confluence.sso.config.IdPConfig;
import com.example.confluence.sso.config.IdPSelector;
import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.util.SSOSessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the "Choose your Identity Provider" page when more than one
 * enabled IdP is configured and no domain-hint resolves the choice automatically.
 *
 * <p>Also handles the email-hint form: when the user submits their email address,
 * it attempts a domain-hint lookup and redirects to the appropriate IdP login
 * endpoint, or falls back to a button list.
 *
 * URL: {@code /plugins/servlet/sso/pick}
 */
public class IdPPickerServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(IdPPickerServlet.class);

    private final SSOConfigManager configManager;
    private final IdPSelector      idpSelector;
    private final TemplateRenderer renderer;

    public IdPPickerServlet(SSOConfigManager configManager,
                            IdPSelector idpSelector,
                            TemplateRenderer renderer) {
        this.configManager = configManager;
        this.idpSelector   = idpSelector;
        this.renderer      = renderer;
    }

    /** Show the picker page. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig global = configManager.load();
        if (!global.isEnabled()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SSO is not enabled.");
            return;
        }

        String returnTo = coalesce(req.getParameter("returnTo"), global.getDefaultRedirectPath());
        SSOSessionUtil.setReturnTo(req, returnTo);

        List<IdPConfig> idps = idpSelector.enabledIdPs();
        if (idps.isEmpty()) {
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "No Identity Providers are configured.");
            return;
        }
        if (idps.size() == 1) {
            // Only one — skip the picker and go directly
            res.sendRedirect(loginUrlFor(idps.get(0), returnTo));
            return;
        }

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("idps",     idps);
        ctx.put("returnTo", returnTo);

        res.setContentType("text/html;charset=UTF-8");
        renderer.render("templates/idp-picker.vm", ctx, res.getWriter());
    }

    /**
     * Handle the email-hint form POST.
     * Tries to resolve the IdP from the submitted email domain.
     * If found, redirects directly to that IdP's login.
     * If not, re-renders the picker with an error.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig global = configManager.load();
        String returnTo = coalesce(SSOSessionUtil.getReturnTo(req), global.getDefaultRedirectPath());
        String email    = req.getParameter("email");

        if (email != null && email.contains("@")) {
            IdPConfig idp = idpSelector.enabledIdPs().stream()
                .filter(c -> c.matchesDomain(email.substring(email.lastIndexOf('@') + 1).toLowerCase()))
                .findFirst().orElse(null);

            if (idp != null) {
                log.debug("Email-hint resolved '{}' to IdP '{}'", email, idp.getId());
                res.sendRedirect(loginUrlFor(idp, returnTo));
                return;
            }
        }

        // No match — re-render with a message
        List<IdPConfig> idps = idpSelector.enabledIdPs();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("idps",    idps);
        ctx.put("returnTo", returnTo);
        ctx.put("error",   "No Identity Provider found for that email address. Please select one below.");
        ctx.put("email",   email);

        res.setContentType("text/html;charset=UTF-8");
        renderer.render("templates/idp-picker.vm", ctx, res.getWriter());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String loginUrlFor(IdPConfig idp, String returnTo) {
        String base = idp.getProtocol() == IdPConfig.Protocol.OIDC
            ? "/plugins/servlet/sso/oidc/login"
            : "/plugins/servlet/sso/saml/login";
        return base + "?idp=" + idp.getId()
            + (returnTo != null && !returnTo.isBlank() ? "&returnTo=" + encode(returnTo) : "");
    }

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
