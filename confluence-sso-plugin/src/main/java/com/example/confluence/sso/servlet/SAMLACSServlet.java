package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.saml.SAMLException;
import com.example.confluence.sso.saml.SAMLHandler;
import com.example.confluence.sso.saml.SAMLUserInfo;
import com.example.confluence.sso.util.SSOSessionUtil;
import com.example.confluence.sso.util.UserProvisioningService;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SAML 2.0 Assertion Consumer Service (ACS).
 *
 * Receives the HTTP-POST response from the IdP, validates the assertion,
 * provisions the user if needed, and establishes the Confluence session.
 *
 * URL: {@code /plugins/servlet/sso/saml/acs}
 */
public class SAMLACSServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SAMLACSServlet.class);

    private final SSOConfigManager       configManager;
    private final UserProvisioningService provisioningService;

    public SAMLACSServlet(SSOConfigManager configManager,
                          UserProvisioningService provisioningService) {
        this.configManager       = configManager;
        this.provisioningService = provisioningService;
    }

    /**
     * IdP posts the SAMLResponse here.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig config = configManager.load();

        if (!config.isEnabled() || config.getProtocol() != SSOConfig.Protocol.SAML) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SAML SSO is not enabled.");
            return;
        }

        String samlResponse = req.getParameter("SAMLResponse");
        String relayState   = req.getParameter("RelayState");

        if (samlResponse == null || samlResponse.isBlank()) {
            log.warn("ACS received request with no SAMLResponse parameter.");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing SAMLResponse.");
            return;
        }

        // Validate relay state to prevent CSRF
        String expectedState = SSOSessionUtil.getState(req);
        if (expectedState != null && !expectedState.equals(relayState)) {
            log.warn("SAML relay state mismatch (expected={}, got={})", expectedState, relayState);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "RelayState mismatch.");
            return;
        }

        try {
            SAMLHandler   handler  = new SAMLHandler(config);
            SAMLUserInfo  userInfo = handler.parseAndValidateResponse(samlResponse);

            log.info("SAML assertion validated for: {}", userInfo);

            // Find or provision the Confluence user
            User user = provisioningService.findOrProvision(
                userInfo.toUsername(),
                userInfo.getEmail(),
                userInfo.getFullName(),
                userInfo.getGroups(),
                config);

            if (user == null) {
                log.error("Cannot resolve Confluence user for SSO identity: {}", userInfo.getNameId());
                res.sendRedirect("/login.action?permissionViolation=true");
                return;
            }

            // Store SSO session attributes
            SSOSessionUtil.setSSOUser(req, user.getName());
            SSOSessionUtil.setSamlNameId(req, userInfo.getNameId());
            SSOSessionUtil.setSamlSessionIndex(req, userInfo.getSessionIndex());

            String returnTo = SSOSessionUtil.consumeReturnTo(req, config.getDefaultRedirectPath());
            log.debug("SAML login success for '{}', redirecting to '{}'", user.getName(), returnTo);
            res.sendRedirect(returnTo);

        } catch (SAMLException e) {
            log.error("SAML response validation failed: {}", e.getMessage(), e);
            res.sendRedirect("/login.action?os_destination=%2F&permissionViolation=true");
        } catch (Exception e) {
            log.error("Unexpected error processing SAML ACS", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSO error: " + e.getMessage());
        }
    }
}
