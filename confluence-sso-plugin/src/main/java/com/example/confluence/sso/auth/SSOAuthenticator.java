package com.example.confluence.sso.auth;

import com.example.confluence.sso.config.SSOConfig;
import com.example.confluence.sso.config.SSOConfigManager;
import com.example.confluence.sso.util.SSOSessionUtil;
import com.atlassian.confluence.user.ConfluenceAuthenticator;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

/**
 * Custom Seraph authenticator that integrates SSO session state with
 * Confluence's built-in authentication lifecycle.
 *
 * <p>Register this class in {@code seraph-config.xml} or rely on the
 * {@link SSOFilter} to redirect users; this authenticator then recognises
 * the session marker set after a successful SSO assertion/token exchange.
 *
 * <pre>{@code
 * <authenticator class="com.example.confluence.sso.auth.SSOAuthenticator"/>
 * }</pre>
 */
public class SSOAuthenticator extends ConfluenceAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(SSOAuthenticator.class);
    private static final long serialVersionUID = 1L;

    // Injected via Spring (Seraph creates authenticators via reflection,
    // so we look up the Spring context manually if needed).
    private transient SSOConfigManager configManager;
    private transient UserAccessor      userAccessor;

    // ── Seraph entry point ────────────────────────────────────────────────────

    @Override
    public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
        // 1. If the standard Confluence session already holds a user, return it.
        Principal existing = super.getUser(request, response);
        if (existing != null) {
            return existing;
        }

        // 2. Check for an SSO-validated user placed in the session by our servlets.
        String ssoUsername = SSOSessionUtil.getSSOUser(request);
        if (ssoUsername == null) {
            return null;
        }

        // 3. Look up the Confluence user object.
        UserAccessor ua = getUserAccessor();
        if (ua == null) {
            log.error("SSOAuthenticator: UserAccessor not available — cannot resolve user '{}'", ssoUsername);
            return null;
        }

        User user = ua.getUserByName(ssoUsername);
        if (user == null) {
            log.warn("SSOAuthenticator: no Confluence user found for SSO username '{}'", ssoUsername);
            return null;
        }

        // 4. Log the user into the Confluence session.
        authoriseUserAndEstablishSession(request, response, user);
        log.debug("SSOAuthenticator: established session for '{}'", ssoUsername);
        return user;
    }

    /**
     * Establish a full Confluence login session for the given user.
     * Delegates to ConfluenceAuthenticator's protected helper so that
     * audit logging and "remember me" cookies work correctly.
     */
    protected void authoriseUserAndEstablishSession(
            HttpServletRequest request, HttpServletResponse response, User user) {
        try {
            // putPrincipalInSessionContext is the Seraph API for this.
            putPrincipalInSessionContext(request, user);
        } catch (Exception e) {
            log.error("SSOAuthenticator: failed to establish session for '{}'", user.getName(), e);
        }
    }

    // ── Lazy Spring context lookup ─────────────────────────────────────────────

    private UserAccessor getUserAccessor() {
        if (userAccessor == null) {
            try {
                userAccessor = com.atlassian.confluence.setup.settings.SettingsManager
                    .getApplicationContext()
                    .getBean(UserAccessor.class);
            } catch (Exception e) {
                log.error("SSOAuthenticator: cannot obtain UserAccessor from Spring context", e);
            }
        }
        return userAccessor;
    }
}
