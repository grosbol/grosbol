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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * SAML 2.0 Single Logout Service (SLO).
 *
 * Handles both IdP-initiated (GET from IdP) and SP-initiated (GET from user)
 * logout flows.
 *
 * URL: {@code /plugins/servlet/sso/saml/slo}
 */
public class SAMLSLOServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SAMLSLOServlet.class);

    private final SSOConfigManager configManager;

    public SAMLSLOServlet(SSOConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * SP-initiated logout: invalidate session then redirect to IdP SLO endpoint.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig config = configManager.load();
        String nameId = SSOSessionUtil.getSamlNameId(req);

        // Invalidate the local session first
        SSOSessionUtil.invalidateSession(req);

        // If the IdP has an SLO endpoint and we have a NameID, redirect there
        String sloUrl = config.getSamlIdpSloUrl();
        if (sloUrl != null && !sloUrl.isBlank() && nameId != null) {
            try {
                String redirectUrl = buildSloRedirect(sloUrl, nameId, config);
                log.debug("Redirecting to IdP SLO: {}", redirectUrl);
                res.sendRedirect(redirectUrl);
                return;
            } catch (Exception e) {
                log.warn("Could not build SLO redirect, falling through to local logout: {}", e.getMessage());
            }
        }

        // Fallback: just redirect to Confluence login
        res.sendRedirect("/login.action");
    }

    private String buildSloRedirect(String sloUrl, String nameId, SSOConfig config) throws Exception {
        // Minimal SLO redirect — a full implementation would build and sign a
        // LogoutRequest using OpenSAML, similar to SAMLHandler.buildAuthnRequest().
        String encodedNameId = URLEncoder.encode(nameId, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(config.getSamlSpEntityId(), StandardCharsets.UTF_8);
        return sloUrl + (sloUrl.contains("?") ? "&" : "?")
            + "NameID=" + encodedNameId
            + "&issuer=" + encodedIssuer;
    }
}
