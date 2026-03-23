package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.IdPConfig;
import com.example.confluence.sso.config.IdPSelector;
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
 * Initiates an OpenID Connect Authorization Code flow.
 *
 * URL: {@code /plugins/servlet/sso/oidc/login}
 */
public class OIDCLoginServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(OIDCLoginServlet.class);

    private final SSOConfigManager configManager;
    private final IdPSelector      idpSelector;

    public OIDCLoginServlet(SSOConfigManager configManager, IdPSelector idpSelector) {
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
            res.sendRedirect(buildPickerUrl(req));
            return;
        }
        if (idp.getProtocol() != IdPConfig.Protocol.OIDC) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "IdP '" + idp.getId() + "' is configured for SAML, not OIDC.");
            return;
        }

        String returnTo = coalesce(req.getParameter("returnTo"), global.getDefaultRedirectPath());
        SSOSessionUtil.setReturnTo(req, returnTo);

        String state = UUID.randomUUID().toString();
        SSOSessionUtil.setState(req, state);
        SSOSessionUtil.setIdPId(req, idp.getId());

        try {
            String authUrl = new OIDCHandler(idp).buildAuthorizationUrl(state);
            log.debug("OIDC login → IdP '{}'", idp.getId());
            res.sendRedirect(authUrl);
        } catch (Exception e) {
            log.error("Failed to build OIDC authorization URL for IdP '{}'", idp.getId(), e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSO error: " + e.getMessage());
        }
    }

    private String buildPickerUrl(HttpServletRequest req) {
        String returnTo = req.getParameter("returnTo");
        return "/plugins/servlet/sso/pick" + (returnTo != null ? "?returnTo=" + returnTo : "");
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
