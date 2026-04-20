package com.example.confluence.sso.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Thin wrapper around {@link HttpSession} for SSO-specific attributes.
 * All session keys are centralised here to prevent typos.
 */
public final class SSOSessionUtil {

    private SSOSessionUtil() {}

    // ── Session attribute keys ─────────────────────────────────────────────────

    /** The authenticated Confluence username after a successful SSO flow. */
    public static final String ATTR_SSO_USER       = "sso.authenticated.user";

    /** The SAML relay state / OIDC state parameter for the in-flight request. */
    public static final String ATTR_SSO_STATE      = "sso.state";

    /** The returnTo URL stored before starting the SSO flow. */
    public static final String ATTR_RETURN_TO      = "sso.returnTo";

    /** The SAML NameID, used for SLO. */
    public static final String ATTR_SAML_NAME_ID   = "sso.saml.nameId";

    /** The SAML session index, used for SLO. */
    public static final String ATTR_SAML_SESSION_INDEX = "sso.saml.sessionIndex";

    /** The IdP id that initiated the current SSO flow. */
    public static final String ATTR_IDP_ID = "sso.idpId";

    // ── Setters ────────────────────────────────────────────────────────────────

    public static void setSSOUser(HttpServletRequest req, String username) {
        req.getSession(true).setAttribute(ATTR_SSO_USER, username);
    }

    public static void setState(HttpServletRequest req, String state) {
        req.getSession(true).setAttribute(ATTR_SSO_STATE, state);
    }

    public static void setReturnTo(HttpServletRequest req, String returnTo) {
        req.getSession(true).setAttribute(ATTR_RETURN_TO, returnTo);
    }

    public static void setSamlNameId(HttpServletRequest req, String nameId) {
        req.getSession(true).setAttribute(ATTR_SAML_NAME_ID, nameId);
    }

    public static void setSamlSessionIndex(HttpServletRequest req, String sessionIndex) {
        req.getSession(true).setAttribute(ATTR_SAML_SESSION_INDEX, sessionIndex);
    }

    public static void setIdPId(HttpServletRequest req, String idpId) {
        req.getSession(true).setAttribute(ATTR_IDP_ID, idpId);
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public static String getSSOUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute(ATTR_SSO_USER) : null;
    }

    public static String getState(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute(ATTR_SSO_STATE) : null;
    }

    public static String getReturnTo(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute(ATTR_RETURN_TO) : null;
    }

    public static String getSamlNameId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute(ATTR_SAML_NAME_ID) : null;
    }

    public static String getSamlSessionIndex(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute(ATTR_SAML_SESSION_INDEX) : null;
    }

    public static String getIdPId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (String) session.getAttribute(ATTR_IDP_ID) : null;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    public static boolean isAuthenticated(HttpServletRequest req) {
        return getSSOUser(req) != null;
    }

    /** Clear all SSO-related session attributes (but keep the session). */
    public static void clearSSOAttributes(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(ATTR_SSO_USER);
            session.removeAttribute(ATTR_SSO_STATE);
            session.removeAttribute(ATTR_RETURN_TO);
            session.removeAttribute(ATTR_SAML_NAME_ID);
            session.removeAttribute(ATTR_SAML_SESSION_INDEX);
            session.removeAttribute(ATTR_IDP_ID);
        }
    }

    /** Invalidate the entire session. */
    public static void invalidateSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Consume and return the stored returnTo URL, removing it from the session. */
    public static String consumeReturnTo(HttpServletRequest req, String fallback) {
        HttpSession session = req.getSession(false);
        if (session == null) return fallback;
        String returnTo = (String) session.getAttribute(ATTR_RETURN_TO);
        session.removeAttribute(ATTR_RETURN_TO);
        return (returnTo != null && !returnTo.isBlank()) ? returnTo : fallback;
    }
}
