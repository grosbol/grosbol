'use strict';

const jwt = require('jsonwebtoken');
const TokenManager = require('../utils/TokenManager');

/**
 * JWT Bearer Token Strategy.
 *
 * Validates a JWT from the Authorization header or a cookie,
 * and exposes the decoded payload as req.user.
 *
 * Config:
 *   secret        {string|Buffer}  Signing secret (HS256) OR
 *   publicKey     {string}         Public key for RS256/ES256 verification
 *   algorithms    {string[]}       Allowed algorithms (default: ['HS256'])
 *   issuer        {string}         Expected "iss" claim
 *   audience      {string}         Expected "aud" claim
 *   tokenFromRequest {function}    Custom extractor: (req) => token|null
 *   onSuccess     {function}       (req, res, payload) — called after verify
 *   onFailure     {function}       (req, res, error)
 */
class JWTStrategy {
  constructor(config) {
    this._validateConfig(config);
    this.config = {
      algorithms: ['HS256'],
      ...config,
    };
    this.tokenManager = new TokenManager(config.secret || config.publicKey);
  }

  _validateConfig(config) {
    if (!config.secret && !config.publicKey) {
      throw new Error('JWTStrategy: either "secret" or "publicKey" is required.');
    }
  }

  _extractToken(req) {
    if (typeof this.config.tokenFromRequest === 'function') {
      return this.config.tokenFromRequest(req);
    }

    // Authorization: Bearer <token>
    const authHeader = req.headers['authorization'];
    if (authHeader && authHeader.startsWith('Bearer ')) {
      return authHeader.slice(7);
    }

    // Cookie fallback
    if (req.cookies && req.cookies.access_token) {
      return req.cookies.access_token;
    }

    return null;
  }

  /**
   * Middleware: extract + verify the JWT and set req.user.
   * Does NOT redirect — suitable for API routes.
   */
  authenticate(options = {}) {
    return (req, res, next) => {
      const token = this._extractToken(req);

      if (!token) {
        const err = new Error('No authentication token provided.');
        err.status = 401;
        if (typeof this.config.onFailure === 'function') {
          return this.config.onFailure(req, res, err);
        }
        return next(err);
      }

      const verifyOptions = {
        algorithms: this.config.algorithms,
      };
      if (this.config.issuer) verifyOptions.issuer = this.config.issuer;
      if (this.config.audience) verifyOptions.audience = this.config.audience;

      const keyOrSecret = this.config.publicKey || this.config.secret;

      jwt.verify(token, keyOrSecret, verifyOptions, (err, payload) => {
        if (err) {
          const authErr = new Error(`Token verification failed: ${err.message}`);
          authErr.status = 401;
          if (typeof this.config.onFailure === 'function') {
            return this.config.onFailure(req, res, authErr);
          }
          return next(authErr);
        }

        req.user = payload;

        if (typeof this.config.onSuccess === 'function') {
          return this.config.onSuccess(req, res, payload);
        }

        next();
      });
    };
  }

  /**
   * Alias so JWTStrategy can be used with SSOManager.
   */
  callback(options = {}) {
    return this.authenticate(options);
  }

  /**
   * Issue a signed JWT for a given payload.
   */
  sign(payload, options = {}) {
    const signOptions = {
      algorithm: this.config.algorithms[0],
      expiresIn: options.expiresIn || '1h',
    };
    if (this.config.issuer) signOptions.issuer = this.config.issuer;
    if (this.config.audience) signOptions.audience = this.config.audience;

    return jwt.sign(payload, this.config.secret, signOptions);
  }
}

module.exports = JWTStrategy;
