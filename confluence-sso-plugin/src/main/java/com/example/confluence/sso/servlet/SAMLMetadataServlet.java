package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.IdPConfig;
import com.example.confluence.sso.config.IdPRegistry;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.saml.SAMLHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Serves SP metadata XML for a specific IdP, identified by {@code ?idp=<id>}.
 *
 * URL: {@code /plugins/servlet/sso/saml/metadata?idp=<id>}
 */
public class SAMLMetadataServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SAMLMetadataServlet.class);

    private final SSOConfigManager configManager;
    private final IdPRegistry      idpRegistry;

    public SAMLMetadataServlet(SSOConfigManager configManager, IdPRegistry idpRegistry) {
        this.configManager = configManager;
        this.idpRegistry   = idpRegistry;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!configManager.load().isEnabled()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SSO is not enabled.");
            return;
        }

        String idpId = req.getParameter("idp");
        IdPConfig idp = idpId != null ? idpRegistry.findById(idpId) : null;

        // Fall back to the single IdP if there is exactly one
        if (idp == null) {
            java.util.List<IdPConfig> all = idpRegistry.loadEnabled();
            if (all.size() == 1 && all.get(0).getProtocol() == IdPConfig.Protocol.SAML) {
                idp = all.get(0);
            }
        }

        if (idp == null || idp.getProtocol() != IdPConfig.Protocol.SAML) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Specify ?idp=<id> for a SAML IdP. Available SAML IdPs: " + samlIdList());
            return;
        }

        try {
            byte[] metadata = new SAMLHandler(idp).generateSpMetadata()
                .getBytes(StandardCharsets.UTF_8);
            res.setContentType("application/xml");
            res.setCharacterEncoding("UTF-8");
            res.setContentLength(metadata.length);
            res.getOutputStream().write(metadata);
        } catch (Exception e) {
            log.error("Failed to generate SP metadata for IdP '{}'", idp.getId(), e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to generate metadata: " + e.getMessage());
        }
    }

    private String samlIdList() {
        StringBuilder sb = new StringBuilder();
        for (IdPConfig c : idpRegistry.loadEnabled()) {
            if (c.getProtocol() == IdPConfig.Protocol.SAML) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(c.getId());
            }
        }
        return sb.toString();
    }
}
