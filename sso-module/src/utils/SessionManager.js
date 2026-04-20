'use strict';

/**
 * SessionManager — thin wrapper around express-session for SSO session handling.
 *
 * Provides helpers to read/write the SSO user object and check auth state.
 */
class SessionManager {
  /**
   * Return an express-session middleware configured for SSO.
   *
   * Options mirror express-session options, with sensible SSO defaults.
   */
  static middleware(options = {}) {
    const session = require('express-session');

    return session({
      secret: options.secret || process.env.SESSION_SECRET || 'changeme',
      resave: false,
      saveUninitialized: false,
      cookie: {
        httpOnly: true,
        secure: options.secure !== undefined ? options.secure : process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        maxAge: options.maxAge || 8 * 60 * 60 * 1000, // 8 hours
      },
      ...options,
    });
  }

  /**
   * Set the authenticated user on the session.
   */
  static setUser(req, user) {
    req.session.user = user;
  }

  /**
   * Retrieve the authenticated user from the session.
   */
  static getUser(req) {
    return req.session && req.session.user;
  }

  /**
   * Check whether the request is authenticated.
   */
  static isAuthenticated(req) {
    return !!(req.session && req.session.user);
  }

  /**
   * Destroy the session (logout).
   */
  static destroy(req) {
    return new Promise((resolve, reject) => {
      req.session.destroy((err) => {
        if (err) return reject(err);
        resolve();
      });
    });
  }

  /**
   * Regenerate the session ID (call after login to prevent session fixation).
   */
  static regenerate(req) {
    return new Promise((resolve, reject) => {
      req.session.regenerate((err) => {
        if (err) return reject(err);
        resolve();
      });
    });
  }
}

module.exports = SessionManager;
