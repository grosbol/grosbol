package com.example.confluence.sso.config;

import com.atlassian.plugin.spring.scanner.annotation.component.ConfluenceComponent;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Determines which {@link IdPConfig} to use for an incoming SSO request.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>{@code idp} query/form parameter — explicit IdP id chosen by the user or set by the app.</li>
 *   <li>{@code login_hint} / {@code email} parameter — domain-hint lookup against all IdPs.</li>
 *   <li>Automatic selection when exactly one enabled IdP is configured.</li>
 *   <li>{@code null} — caller must show the IdP picker page.</li>
 * </ol>
 */
@ConfluenceComponent
public class IdPSelector {

    private final IdPRegistry registry;

    @Autowired
    public IdPSelector(IdPRegistry registry) {
        this.registry = registry;
    }

    /**
     * Resolve the IdP for a request, or return {@code null} if the picker page is needed.
     */
    public IdPConfig resolve(HttpServletRequest req) {
        // 1. Explicit idp= parameter
        String idpId = req.getParameter("idp");
        if (idpId != null && !idpId.isBlank()) {
            IdPConfig idp = registry.findById(idpId);
            if (idp != null && idp.isEnabled()) return idp;
        }

        // 2. Domain hint from login_hint or email parameter
        String hint = coalesce(req.getParameter("login_hint"), req.getParameter("email"));
        if (hint != null && hint.contains("@")) {
            IdPConfig idp = registry.findByEmailDomain(hint);
            if (idp != null) return idp;
        }

        // 3. Auto-select if only one IdP is configured
        return registry.defaultIfSingle();
    }

    /**
     * Resolve by explicit id only (used in ACS/callback where the IdP is known).
     */
    public IdPConfig resolveById(String idpId) {
        return registry.findById(idpId);
    }

    /** Returns all enabled IdPs for rendering the picker page. */
    public List<IdPConfig> enabledIdPs() {
        return registry.loadEnabled();
    }

    private String coalesce(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
