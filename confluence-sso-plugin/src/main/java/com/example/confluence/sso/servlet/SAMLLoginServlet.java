package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.IdPConfig;
import com.example.confluence.sso.config.IdPSelector;
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
 * Initiates a SAML 2.0 SSO flow.
 *
 * Requires either:
 * <ul>
 *   <li>{@code ?idp=<id>} — explicit IdP selection, OR</li>
 *   <li>automatic selection when only one enabled SAML IdP exists.</li>
 * </ul>
 *
 * URL: {@code /plugins/servlet/sso/saml/login}
 */
public class SAMLLoginServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SAMLLoginServlet.class);

    private final SSOConfigManager configManager;
    private final IdPSelector      idpSelector;

    public SAMLLoginServlet(SSOConfigManager configManager, IdPSelector idpSelector) {
        this.configManager = configManager;
        this.idpSelector   = idpSelector;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig global = configManager.load();
        if (!global.isEnabled()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SSO is not enabled.");
            return;
        }

        IdPConfig idp = idpSelector.resolve(req);
        if (idp == null) {
            // Multiple IdPs — redirect to the picker
            res.sendRedirect(buildPickerUrl(req));
            return;
        }
        if (idp.getProtocol() != IdPConfig.Protocol.SAML) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "IdP '" + idp.getId() + "' is configured for OIDC, not SAML.");
            return;
        }

        String returnTo = coalesce(req.getParameter("returnTo"), global.getDefaultRedirectPath());
        SSOSessionUtil.setReturnTo(req, returnTo);

        String relayState = UUID.randomUUID().toString();
        SSOSessionUtil.setState(req, relayState);
        SSOSessionUtil.setIdPId(req, idp.getId());

        try {
            SAMLHandler.SAMLRequestData data = new SAMLHandler(idp).buildAuthnRequest(relayState);
            log.debug("SAML login → IdP '{}' (requestId={})", idp.getId(), data.requestId);
            res.sendRedirect(data.redirectUrl);
        } catch (Exception e) {
            log.error("Failed to build SAML AuthnRequest for IdP '{}'", idp.getId(), e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSO error: " + e.getMessage());
        }
    }

    private String buildPickerUrl(HttpServletRequest req) {
        String returnTo = req.getParameter("returnTo");
        String base = "/plugins/servlet/sso/pick";
        return returnTo != null ? base + "?returnTo=" + returnTo : base;
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
