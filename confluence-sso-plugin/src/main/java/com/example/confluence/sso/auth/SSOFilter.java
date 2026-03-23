package com.example.confluence.sso.auth;

import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.util.SSOSessionUtil;
import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Seraph-level servlet filter that enforces SSO on every request.
 *
 * <p>Flow:
 * <ol>
 *   <li>Skip public / static / SSO-own endpoints.</li>
 *   <li>If the session already has an authenticated Confluence user, continue.</li>
 *   <li>If forceSSO is enabled and no session, redirect to the SSO login endpoint.</li>
 *   <li>Otherwise let Confluence's own login page handle it.</li>
 * </ol>
 */
@ConfluenceComponent
public class SSOFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SSOFilter.class);

    /** Paths that must never trigger an SSO redirect. */
    private static final Set<String> BYPASS_PATHS = Set.of(
        "/plugins/servlet/sso/saml/login",
        "/plugins/servlet/sso/saml/acs",
        "/plugins/servlet/sso/saml/metadata",
        "/plugins/servlet/sso/saml/slo",
        "/plugins/servlet/sso/oidc/login",
        "/plugins/servlet/sso/oidc/callback",
        "/plugins/servlet/sso/logout",
        "/plugins/servlet/sso/admin",
        "/plugins/servlet/sso/pick",
        "/login.action",
        "/dologin.action"
    );

    private final SSOConfigManager configManager;
    private String loginUrl;

    @Autowired
    public SSOFilter(SSOConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        loginUrl = filterConfig.getInitParameter("loginURL");
        if (loginUrl == null || loginUrl.isBlank()) {
            loginUrl = "/plugins/servlet/sso/saml/login";
        }
        log.info("SSOFilter initialised (loginUrl={})", loginUrl);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        SSOConfig config = configManager.load();

        // ── Plugin disabled — do nothing ──────────────────────────────────────
        if (!config.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String path = req.getServletPath();

        // ── Always bypass SSO-own and login paths ─────────────────────────────
        if (isBypassPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // ── Skip static resources ─────────────────────────────────────────────
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        // ── Already authenticated in this session ─────────────────────────────
        if (SSOSessionUtil.isAuthenticated(req)) {
            chain.doFilter(request, response);
            return;
        }

        // ── Force SSO: redirect to IdP ────────────────────────────────────────
        if (config.isForceSSO()) {
            String returnTo = encodeReturnTo(req);
            String redirect = buildLoginUrl(config, returnTo);
            log.debug("SSOFilter: unauthenticated request to {} — redirecting to {}", path, redirect);
            res.sendRedirect(redirect);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() { /* nothing */ }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBypassPath(String path) {
        if (path == null) return false;
        for (String bypass : BYPASS_PATHS) {
            if (path.startsWith(bypass)) return true;
        }
        return false;
    }

    private boolean isStaticResource(String path) {
        if (path == null) return false;
        return path.startsWith("/s/") ||
               path.startsWith("/download/") ||
               path.endsWith(".css") ||
               path.endsWith(".js")  ||
               path.endsWith(".png") ||
               path.endsWith(".ico");
    }

    private String encodeReturnTo(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String qs  = req.getQueryString();
        String full = (qs != null && !qs.isEmpty()) ? uri + "?" + qs : uri;
        try {
            return java.net.URLEncoder.encode(full, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "/";
        }
    }

    private String buildLoginUrl(SSOConfig config, String returnTo) {
        String base = config.getProtocol() == SSOConfig.Protocol.OIDC
            ? "/plugins/servlet/sso/oidc/login"
            : "/plugins/servlet/sso/saml/login";
        return base + "?returnTo=" + returnTo;
    }
}
