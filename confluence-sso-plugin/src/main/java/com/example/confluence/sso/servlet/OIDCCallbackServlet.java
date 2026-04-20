package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.IdPConfig;
import com.example.confluence.sso.config.IdPSelector;
import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.oidc.OIDCException;
import com.example.confluence.sso.oidc.OIDCHandler;
import com.example.confluence.sso.oidc.OIDCUserInfo;
import com.example.confluence.sso.util.SSOSessionUtil;
import com.example.confluence.sso.util.UserProvisioningService;
import com.atlassian.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles the OIDC authorization-server redirect back to Confluence.
 *
 * The IdP is identified from the session (set by {@link OIDCLoginServlet}).
 * Each OIDC IdP should have its own redirect URI configured in the IdP application
 * (e.g. {@code /plugins/servlet/sso/oidc/callback?idp=okta}), or rely on the
 * session-stored IdP id when a shared callback URL is used.
 *
 * URL: {@code /plugins/servlet/sso/oidc/callback}
 */
public class OIDCCallbackServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OIDCCallbackServlet.class);

    private final SSOConfigManager       configManager;
    private final IdPSelector            idpSelector;
    private final UserProvisioningService provisioningService;

    public OIDCCallbackServlet(SSOConfigManager configManager,
                               IdPSelector idpSelector,
                               UserProvisioningService provisioningService) {
        this.configManager       = configManager;
        this.idpSelector         = idpSelector;
        this.provisioningService = provisioningService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig global = configManager.load();
        if (!global.isEnabled()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SSO is not enabled.");
            return;
        }

        // Error from IdP
        String error = req.getParameter("error");
        if (error != null) {
            log.warn("OIDC error from IdP: {} — {}", error, req.getParameter("error_description"));
            res.sendRedirect("/login.action?permissionViolation=true");
            return;
        }

        String code  = req.getParameter("code");
        String state = req.getParameter("state");
        if (code == null || state == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing code or state.");
            return;
        }

        // Validate state
        String expectedState = SSOSessionUtil.getState(req);
        if (!state.equals(expectedState)) {
            log.warn("OIDC state mismatch (expected={}, got={})", expectedState, state);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "State mismatch.");
            return;
        }

        // Resolve IdP — prefer session, fall back to ?idp= query param
        String idpId = coalesce(SSOSessionUtil.getIdPId(req), req.getParameter("idp"));
        IdPConfig idp = idpId != null ? idpSelector.resolveById(idpId) : null;
        if (idp == null) {
            log.error("OIDC callback: cannot resolve IdP (idpId={})", idpId);
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown IdP for this SSO session.");
            return;
        }

        try {
            OIDCUserInfo userInfo = new OIDCHandler(idp).exchangeCodeAndFetchUser(code, state);
            log.info("OIDC token exchanged via IdP '{}' for: {}", idp.getId(), userInfo);

            User user = provisioningService.findOrProvision(
                userInfo.toUsername(), userInfo.getEmail(),
                userInfo.getFullName(), userInfo.getGroups(), global);

            if (user == null) {
                log.error("Cannot resolve Confluence user for OIDC subject '{}' via IdP '{}'",
                    userInfo.getSubject(), idp.getId());
                res.sendRedirect("/login.action?permissionViolation=true");
                return;
            }

            SSOSessionUtil.setSSOUser(req, user.getName());

            String returnTo = SSOSessionUtil.consumeReturnTo(req, global.getDefaultRedirectPath());
            log.debug("OIDC login success for '{}' via IdP '{}', → '{}'",
                user.getName(), idp.getId(), returnTo);
            res.sendRedirect(returnTo);

        } catch (OIDCException e) {
            log.error("OIDC callback failed for IdP '{}': {}", idp.getId(), e.getMessage(), e);
            res.sendRedirect("/login.action?permissionViolation=true");
        } catch (Exception e) {
            log.error("Unexpected error in OIDC callback for IdP '{}'", idp.getId(), e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSO error: " + e.getMessage());
        }
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
