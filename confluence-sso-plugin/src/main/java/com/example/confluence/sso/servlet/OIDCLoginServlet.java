package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.oidc.OIDCHandler;
import com.example.confluence.sso.util.SSOSessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Initiates an OpenID Connect / OAuth 2.0 Authorization Code flow.
 *
 * URL: {@code /plugins/servlet/sso/oidc/login}
 */
public class OIDCLoginServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OIDCLoginServlet.class);

    private final SSOConfigManager configManager;

    public OIDCLoginServlet(SSOConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig config = configManager.load();

        if (!config.isEnabled() || config.getProtocol() != SSOConfig.Protocol.OIDC) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "OIDC SSO is not enabled.");
            return;
        }

        String returnTo = req.getParameter("returnTo");
        if (returnTo == null || returnTo.isBlank()) {
            returnTo = config.getDefaultRedirectPath();
        }

        SSOSessionUtil.setReturnTo(req, returnTo);

        String state = UUID.randomUUID().toString();
        SSOSessionUtil.setState(req, state);

        try {
            OIDCHandler handler = new OIDCHandler(config);
            String authUrl = handler.buildAuthorizationUrl(state);
            log.debug("Redirecting to OIDC authorization endpoint.");
            res.sendRedirect(authUrl);
        } catch (Exception e) {
            log.error("Failed to build OIDC authorization URL", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to initiate OIDC: " + e.getMessage());
        }
    }
}
