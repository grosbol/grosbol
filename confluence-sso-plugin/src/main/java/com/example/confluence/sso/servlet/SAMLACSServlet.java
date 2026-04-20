package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.IdPConfig;
import com.example.confluence.sso.config.IdPSelector;
import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.saml.SAMLException;
import com.example.confluence.sso.saml.SAMLHandler;
import com.example.confluence.sso.saml.SAMLUserInfo;
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
 * SAML 2.0 Assertion Consumer Service.
 *
 * The IdP that initiated the flow is identified via the session attribute
 * set by {@link SAMLLoginServlet} (key {@code sso.idpId}).
 * This avoids any ambiguity when multiple IdPs share the same ACS URL.
 *
 * URL: {@code /plugins/servlet/sso/saml/acs}
 */
public class SAMLACSServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SAMLACSServlet.class);

    private final SSOConfigManager       configManager;
    private final IdPSelector            idpSelector;
    private final UserProvisioningService provisioningService;

    public SAMLACSServlet(SSOConfigManager configManager,
                          IdPSelector idpSelector,
                          UserProvisioningService provisioningService) {
        this.configManager       = configManager;
        this.idpSelector         = idpSelector;
        this.provisioningService = provisioningService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig global = configManager.load();
        if (!global.isEnabled()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SSO is not enabled.");
            return;
        }

        String samlResponse = req.getParameter("SAMLResponse");
        String relayState   = req.getParameter("RelayState");

        if (samlResponse == null || samlResponse.isBlank()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing SAMLResponse.");
            return;
        }

        // Validate relay state
        String expectedState = SSOSessionUtil.getState(req);
        if (expectedState != null && !expectedState.equals(relayState)) {
            log.warn("SAML relay state mismatch (expected={}, got={})", expectedState, relayState);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "RelayState mismatch.");
            return;
        }

        // Identify which IdP we are talking to
        String idpId = SSOSessionUtil.getIdPId(req);
        IdPConfig idp = idpId != null ? idpSelector.resolveById(idpId) : null;
        if (idp == null) {
            log.error("ACS: no IdP ID in session — cannot validate assertion.");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown IdP for this SSO session.");
            return;
        }

        try {
            SAMLUserInfo userInfo = new SAMLHandler(idp).parseAndValidateResponse(samlResponse);
            log.info("SAML assertion validated via IdP '{}' for: {}", idp.getId(), userInfo);

            User user = provisioningService.findOrProvision(
                userInfo.toUsername(), userInfo.getEmail(),
                userInfo.getFullName(), userInfo.getGroups(), global);

            if (user == null) {
                log.error("Cannot resolve Confluence user for IdP '{}' identity: {}", idp.getId(), userInfo.getNameId());
                res.sendRedirect("/login.action?permissionViolation=true");
                return;
            }

            SSOSessionUtil.setSSOUser(req, user.getName());
            SSOSessionUtil.setSamlNameId(req, userInfo.getNameId());
            SSOSessionUtil.setSamlSessionIndex(req, userInfo.getSessionIndex());

            String returnTo = SSOSessionUtil.consumeReturnTo(req, global.getDefaultRedirectPath());
            log.debug("SAML login success for '{}' via IdP '{}', → '{}'",
                user.getName(), idp.getId(), returnTo);
            res.sendRedirect(returnTo);

        } catch (SAMLException e) {
            log.error("SAML validation failed for IdP '{}': {}", idp.getId(), e.getMessage(), e);
            res.sendRedirect("/login.action?permissionViolation=true");
        } catch (Exception e) {
            log.error("Unexpected error in SAML ACS for IdP '{}'", idp.getId(), e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSO error: " + e.getMessage());
        }
    }
}
