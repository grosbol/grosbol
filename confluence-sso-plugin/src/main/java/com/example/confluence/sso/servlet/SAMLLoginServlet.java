package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.saml.SAMLHandler;
import com.example.confluence.sso.util.SSOSessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Initiates a SAML 2.0 SSO flow by building an AuthnRequest and
 * redirecting the user to the IdP.
 *
 * URL: {@code /plugins/servlet/sso/saml/login}
 */
public class SAMLLoginServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SAMLLoginServlet.class);

    private final SSOConfigManager configManager;

    public SAMLLoginServlet(SSOConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig config = configManager.load();

        if (!config.isEnabled() || config.getProtocol() != SSOConfig.Protocol.SAML) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SAML SSO is not enabled.");
            return;
        }

        String returnTo = req.getParameter("returnTo");
        if (returnTo == null || returnTo.isBlank()) {
            returnTo = config.getDefaultRedirectPath();
        }

        // Store returnTo for after the ACS servlet completes
        SSOSessionUtil.setReturnTo(req, returnTo);

        // Generate a relay state (opaque ID)
        String relayState = UUID.randomUUID().toString();
        SSOSessionUtil.setState(req, relayState);

        try {
            SAMLHandler handler = new SAMLHandler(config);
            SAMLHandler.SAMLRequestData requestData = handler.buildAuthnRequest(relayState);

            log.debug("Redirecting to IdP for SAML SSO (requestId={})", requestData.requestId);
            res.sendRedirect(requestData.redirectUrl);

        } catch (Exception e) {
            log.error("Failed to build SAML AuthnRequest", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to initiate SSO: " + e.getMessage());
        }
    }
}
