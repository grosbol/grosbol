'use strict';

const axios = require('axios');
const { v4: uuidv4 } = require('uuid');
const TokenManager = require('../utils/TokenManager');

/**
 * OAuth 2.0 Strategy (Authorization Code flow).
 *
 * Config:
 *   clientId        {string}   OAuth client ID
 *   clientSecret    {string}   OAuth client secret
 *   authorizationURL{string}   IdP authorization endpoint
 *   tokenURL        {string}   IdP token endpoint
 *   userInfoURL     {string}   IdP userinfo endpoint
 *   callbackURL     {string}   Registered redirect URI
 *   scope           {string[]} Requested OAuth scopes
 *   onSuccess       {function} Called with (req, res, user, tokens)
 *   onFailure       {function} Called with (req, res, error)
 */
class OAuthStrategy {
  constructor(config) {
    this._validateConfig(config);
    this.config = {
      scope: ['openid', 'profile', 'email'],
      ...config,
    };
    this.tokenManager = new TokenManager(config.jwtSecret);
  }

  _validateConfig(config) {
    const required = ['clientId', 'clientSecret', 'authorizationURL', 'tokenURL', 'callbackURL'];
    for (const field of required) {
      if (!config[field]) {
        throw new Error(`OAuthStrategy: missing required config field "${field}"`);
      }
    }
  }

  /**
   * Step 1 — redirect to the IdP authorization endpoint.
   */
  authenticate(options = {}) {
    return (req, res) => {
      const state = uuidv4();
      req.session.oauthState = state;

      const params = new URLSearchParams({
        response_type: 'code',
        client_id: this.config.clientId,
        redirect_uri: this.config.callbackURL,
        scope: (options.scope || this.config.scope).join(' '),
        state,
      });

      res.redirect(`${this.config.authorizationURL}?${params}`);
    };
  }

  /**
   * Step 2 — exchange the authorization code for tokens and fetch the user.
   */
  callback(options = {}) {
    return async (req, res, next) => {
      try {
        const { code, state, error } = req.query;

        if (error) {
          throw new Error(`IdP returned error: ${error}`);
        }

        if (state !== req.session.oauthState) {
          throw new Error('OAuth state mismatch — possible CSRF attack.');
        }

        // Exchange code for tokens
        const tokenResponse = await axios.post(
          this.config.tokenURL,
          new URLSearchParams({
            grant_type: 'authorization_code',
            code,
            redirect_uri: this.config.callbackURL,
            client_id: this.config.clientId,
            client_secret: this.config.clientSecret,
          }),
          { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
        );

        const tokens = tokenResponse.data;

        // Fetch user profile
        const userResponse = await axios.get(this.config.userInfoURL, {
          headers: { Authorization: `Bearer ${tokens.access_token}` },
        });

        const user = userResponse.data;

        // Store in session
        req.session.user = user;
        req.session.tokens = {
          accessToken: tokens.access_token,
          refreshToken: tokens.refresh_token,
          expiresIn: tokens.expires_in,
        };
        delete req.session.oauthState;

        if (typeof this.config.onSuccess === 'function') {
          return this.config.onSuccess(req, res, user, tokens);
        }

        next();
      } catch (err) {
        if (typeof this.config.onFailure === 'function') {
          return this.config.onFailure(req, res, err);
        }
        next(err);
      }
    };
  }

  logout(options = {}) {
    return (req, res) => {
      req.session.destroy(() => {
        const redirectTo = options.redirectTo || '/';
        res.redirect(redirectTo);
      });
    };
  }
}

module.exports = OAuthStrategy;
