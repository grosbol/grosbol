package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.util.SSOSessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Protocol-agnostic logout entry point.
 *
 * Delegates to the appropriate SLO endpoint based on the current SSO protocol.
 *
 * URL: {@code /plugins/servlet/sso/logout}
 */
public class SSOLogoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SSOLogoutServlet.class);

    private final SSOConfigManager configManager;

    public SSOLogoutServlet(SSOConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig config = configManager.load();

        if (config.isEnabled() && config.getProtocol() == SSOConfig.Protocol.SAML) {
            // Delegate to SAML SLO
            res.sendRedirect("/plugins/servlet/sso/saml/slo");
            return;
        }

        if (config.isEnabled() && config.getProtocol() == SSOConfig.Protocol.OIDC) {
            // For OIDC we only do local logout (RP-initiated logout requires
            // id_token_hint which we'd need to store in session)
            SSOSessionUtil.invalidateSession(req);
            log.debug("OIDC local session invalidated.");
            res.sendRedirect("/login.action");
            return;
        }

        // Fallback: plain local logout
        SSOSessionUtil.invalidateSession(req);
        res.sendRedirect("/login.action");
    }
}
