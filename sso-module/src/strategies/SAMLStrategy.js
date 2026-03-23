'use strict';

const samlify = require('samlify');
const fs = require('fs');

/**
 * SAML 2.0 Strategy (Service Provider side).
 *
 * Config:
 *   sp {object}
 *     entityID       {string}  SP entity ID
 *     assertionConsumerService [{binding, location}]
 *     privateKey     {string}  Path to SP private key PEM
 *     certificate    {string}  Path to SP certificate PEM
 *
 *   idp {object}
 *     entityID       {string}  IdP entity ID
 *     singleSignOnService [{binding, location}]
 *     certificate    {string}  Path to IdP certificate PEM
 *     // OR:
 *     metadataPath   {string}  Path to IdP metadata XML file
 *
 *   onSuccess  {function} (req, res, user)
 *   onFailure  {function} (req, res, error)
 */
class SAMLStrategy {
  constructor(config) {
    this._validateConfig(config);
    this.config = config;
    this._sp = null;
    this._idp = null;
  }

  _validateConfig(config) {
    if (!config.sp || !config.idp) {
      throw new Error('SAMLStrategy: "sp" and "idp" configuration objects are required.');
    }
  }

  _getSP() {
    if (!this._sp) {
      const spConfig = { ...this.config.sp };
      if (spConfig.privateKey) {
        spConfig.privateKey = fs.readFileSync(spConfig.privateKey, 'utf8');
      }
      if (spConfig.certificate) {
        spConfig.certificate = fs.readFileSync(spConfig.certificate, 'utf8');
      }
      this._sp = samlify.ServiceProvider(spConfig);
    }
    return this._sp;
  }

  _getIdP() {
    if (!this._idp) {
      const idpConfig = { ...this.config.idp };
      if (idpConfig.metadataPath) {
        const metadata = fs.readFileSync(idpConfig.metadataPath, 'utf8');
        this._idp = samlify.IdentityProvider({ metadata });
      } else {
        if (idpConfig.certificate) {
          idpConfig.certificate = fs.readFileSync(idpConfig.certificate, 'utf8');
        }
        this._idp = samlify.IdentityProvider(idpConfig);
      }
    }
    return this._idp;
  }

  /**
   * Step 1 — generate a SAML AuthnRequest and redirect to the IdP.
   */
  authenticate(options = {}) {
    return async (req, res, next) => {
      try {
        const sp = this._getSP();
        const idp = this._getIdP();
        const { context } = sp.createLoginRequest(idp, 'redirect');
        res.redirect(context);
      } catch (err) {
        next(err);
      }
    };
  }

  /**
   * Step 2 — parse and validate the SAML response (HTTP-POST binding).
   */
  callback(options = {}) {
    return async (req, res, next) => {
      try {
        const sp = this._getSP();
        const idp = this._getIdP();

        const { extract } = await sp.parseLoginResponse(idp, 'post', req);

        const user = {
          nameID: extract.nameID,
          sessionIndex: extract.sessionIndex,
          attributes: extract.attributes || {},
        };

        req.session.user = user;

        if (typeof this.config.onSuccess === 'function') {
          return this.config.onSuccess(req, res, user);
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
   * Generate SAML SP metadata XML (useful for registering with the IdP).
   */
  metadata() {
    return (req, res) => {
      const sp = this._getSP();
      res.header('Content-Type', 'text/xml').send(sp.getMetadata());
    };
  }

  logout(options = {}) {
    return async (req, res, next) => {
      try {
        const sp = this._getSP();
        const idp = this._getIdP();
        const user = req.session.user;

        if (user && user.nameID) {
          const { context } = sp.createLogoutRequest(idp, 'redirect', {
            nameID: user.nameID,
            sessionIndex: user.sessionIndex,
          });
          req.session.destroy(() => res.redirect(context));
        } else {
          req.session.destroy(() => res.redirect(options.redirectTo || '/'));
        }
      } catch (err) {
        next(err);
      }
    };
  }
}

module.exports = SAMLStrategy;
