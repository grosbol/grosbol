'use strict';

const { Issuer, generators } = require('openid-client');
const TokenManager = require('../utils/TokenManager');

/**
 * OpenID Connect Strategy.
 *
 * Builds on top of the openid-client library and performs full OIDC
 * discovery, PKCE, and ID-token validation automatically.
 *
 * Config:
 *   issuerURL    {string}   OIDC issuer URL (used for auto-discovery)
 *   clientId     {string}
 *   clientSecret {string}
 *   callbackURL  {string}
 *   scope        {string[]} default: ['openid', 'profile', 'email']
 *   onSuccess    {function} (req, res, userinfo, tokenset)
 *   onFailure    {function} (req, res, error)
 */
class OIDCStrategy {
  constructor(config) {
    this._validateConfig(config);
    this.config = {
      scope: ['openid', 'profile', 'email'],
      ...config,
    };
    this._client = null; // lazily initialized
    this.tokenManager = new TokenManager(config.jwtSecret);
  }

  _validateConfig(config) {
    const required = ['issuerURL', 'clientId', 'clientSecret', 'callbackURL'];
    for (const field of required) {
      if (!config[field]) {
        throw new Error(`OIDCStrategy: missing required config field "${field}"`);
      }
    }
  }

  async _getClient() {
    if (!this._client) {
      const issuer = await Issuer.discover(this.config.issuerURL);
      this._client = new issuer.Client({
        client_id: this.config.clientId,
        client_secret: this.config.clientSecret,
        redirect_uris: [this.config.callbackURL],
        response_types: ['code'],
      });
    }
    return this._client;
  }

  /**
   * Step 1 — build the authorization URL with PKCE and redirect.
   */
  authenticate(options = {}) {
    return async (req, res, next) => {
      try {
        const client = await this._getClient();
        const codeVerifier = generators.codeVerifier();
        const codeChallenge = generators.codeChallenge(codeVerifier);
        const nonce = generators.nonce();
        const state = generators.state();

        req.session.oidc = { codeVerifier, nonce, state };

        const redirectUrl = client.authorizationUrl({
          scope: (options.scope || this.config.scope).join(' '),
          code_challenge: codeChallenge,
          code_challenge_method: 'S256',
          nonce,
          state,
        });

        res.redirect(redirectUrl);
      } catch (err) {
        next(err);
      }
    };
  }

  /**
   * Step 2 — validate the callback, exchange code, verify ID token.
   */
  callback(options = {}) {
    return async (req, res, next) => {
      try {
        const client = await this._getClient();
        const { codeVerifier, nonce, state } = req.session.oidc || {};

        const params = client.callbackParams(req);
        const tokenSet = await client.callback(this.config.callbackURL, params, {
          code_verifier: codeVerifier,
          nonce,
          state,
        });

        const userInfo = await client.userinfo(tokenSet.access_token);

        req.session.user = userInfo;
        req.session.tokens = {
          accessToken: tokenSet.access_token,
          refreshToken: tokenSet.refresh_token,
          idToken: tokenSet.id_token,
          expiresAt: tokenSet.expires_at,
        };
        delete req.session.oidc;

        if (typeof this.config.onSuccess === 'function') {
          return this.config.onSuccess(req, res, userInfo, tokenSet);
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

  /**
   * Initiate OIDC logout (end-session endpoint).
   */
  logout(options = {}) {
    return async (req, res) => {
      try {
        const client = await this._getClient();
        const idToken = req.session.tokens && req.session.tokens.idToken;
        const postLogoutRedirectUri = options.redirectTo || '/';

        req.session.destroy(async () => {
          const endSessionUrl = client.issuer.metadata.end_session_endpoint;
          if (endSessionUrl && idToken) {
            const params = new URLSearchParams({
              id_token_hint: idToken,
              post_logout_redirect_uri: postLogoutRedirectUri,
            });
            res.redirect(`${endSessionUrl}?${params}`);
          } else {
            res.redirect(postLogoutRedirectUri);
          }
        });
      } catch {
        res.redirect(options.redirectTo || '/');
      }
    };
  }
}

module.exports = OIDCStrategy;
