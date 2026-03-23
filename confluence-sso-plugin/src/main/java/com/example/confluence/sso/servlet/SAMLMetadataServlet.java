package com.example.confluence.sso.servlet;

import com.example.confluence.sso.config.SSOConfig;
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
 * Serves the SP metadata XML needed to register this Confluence instance
 * as a Service Provider in your IdP (Okta, ADFS, Azure AD, etc.).
 *
 * URL: {@code /plugins/servlet/sso/saml/metadata}
 */
public class SAMLMetadataServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SAMLMetadataServlet.class);

    private final SSOConfigManager configManager;

    public SAMLMetadataServlet(SSOConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SSOConfig config = configManager.load();

        if (!config.isEnabled() || config.getProtocol() != SSOConfig.Protocol.SAML) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "SAML SSO is not enabled.");
            return;
        }

        try {
            SAMLHandler handler  = new SAMLHandler(config);
            String      metadata = handler.generateSpMetadata();

            res.setContentType("application/xml");
            res.setCharacterEncoding("UTF-8");
            byte[] bytes = metadata.getBytes(StandardCharsets.UTF_8);
            res.setContentLength(bytes.length);
            res.getOutputStream().write(bytes);

        } catch (Exception e) {
            log.error("Failed to generate SP metadata", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to generate SP metadata: " + e.getMessage());
        }
    }
}
