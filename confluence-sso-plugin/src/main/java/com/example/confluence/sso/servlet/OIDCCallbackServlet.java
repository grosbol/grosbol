package com.example.confluence.sso.servlet;

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
 * Handles the OIDC authorization server redirect back to Confluence.
 *
 * URL: {@code /plugins/servlet/sso/oidc/callback}
 */
public class OIDCCallbackServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OIDCCallbackServlet.class);

    private final SSOConfigManager       configManager;
    private final UserProvisioningService provisioningService;

    public OIDCCallbackServlet(SSOConfigManager configManager,
                               UserProvisioningService provisioningService) {
        this.configManager       = configManager;
        this.provisioningService = provisioningService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig config = configManager.load();

        if (!config.isEnabled() || config.getProtocol() != SSOConfig.Protocol.OIDC) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "OIDC SSO is not enabled.");
            return;
        }

        // Check for error from IdP
        String error = req.getParameter("error");
        if (error != null) {
            String desc = req.getParameter("error_description");
            log.warn("OIDC error from IdP: {} — {}", error, desc);
            res.sendRedirect("/login.action?permissionViolation=true");
            return;
        }

        String code  = req.getParameter("code");
        String state = req.getParameter("state");

        if (code == null || state == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing code or state parameter.");
            return;
        }

        // Validate state
        String expectedState = SSOSessionUtil.getState(req);
        if (!state.equals(expectedState)) {
            log.warn("OIDC state mismatch — possible CSRF (expected={}, got={})", expectedState, state);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "State mismatch.");
            return;
        }

        try {
            OIDCHandler  handler  = new OIDCHandler(config);
            OIDCUserInfo userInfo = handler.exchangeCodeAndFetchUser(code, state);

            log.info("OIDC token exchanged successfully for: {}", userInfo);

            User user = provisioningService.findOrProvision(
                userInfo.toUsername(),
                userInfo.getEmail(),
                userInfo.getFullName(),
                userInfo.getGroups(),
                config);

            if (user == null) {
                log.error("Cannot resolve Confluence user for OIDC subject: {}", userInfo.getSubject());
                res.sendRedirect("/login.action?permissionViolation=true");
                return;
            }

            SSOSessionUtil.setSSOUser(req, user.getName());

            String returnTo = SSOSessionUtil.consumeReturnTo(req, config.getDefaultRedirectPath());
            log.debug("OIDC login success for '{}', redirecting to '{}'", user.getName(), returnTo);
            res.sendRedirect(returnTo);

        } catch (OIDCException e) {
            log.error("OIDC callback failed: {}", e.getMessage(), e);
            res.sendRedirect("/login.action?permissionViolation=true");
        } catch (Exception e) {
            log.error("Unexpected error in OIDC callback", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OIDC error: " + e.getMessage());
        }
    }
}
