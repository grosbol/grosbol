'use strict';

const EventEmitter = require('events');

/**
 * SSOManager — central registry and router for SSO strategies.
 *
 * Usage:
 *   const manager = new SSOManager({ defaultStrategy: 'oidc' });
 *   manager.register('oidc', new OIDCStrategy(config));
 *   app.use('/auth', manager.router());
 */
class SSOManager extends EventEmitter {
  constructor(options = {}) {
    super();
    this.strategies = new Map();
    this.defaultStrategy = options.defaultStrategy || null;
    this.options = options;
  }

  /**
   * Register a strategy under a given name.
   * @param {string} name - Unique identifier for the strategy.
   * @param {object} strategy - Strategy instance (must implement authenticate()).
   */
  register(name, strategy) {
    if (this.strategies.has(name)) {
      throw new Error(`Strategy "${name}" is already registered.`);
    }
    strategy.name = name;
    this.strategies.set(name, strategy);
    this.emit('strategy:registered', { name, strategy });
    return this;
  }

  /**
   * Unregister a strategy by name.
   */
  unregister(name) {
    if (!this.strategies.has(name)) {
      throw new Error(`Strategy "${name}" is not registered.`);
    }
    this.strategies.delete(name);
    this.emit('strategy:unregistered', { name });
    return this;
  }

  /**
   * Retrieve a registered strategy.
   */
  get(name) {
    const strategy = this.strategies.get(name || this.defaultStrategy);
    if (!strategy) {
      throw new Error(`Strategy "${name}" not found.`);
    }
    return strategy;
  }

  /**
   * List all registered strategy names.
   */
  list() {
    return Array.from(this.strategies.keys());
  }

  /**
   * Authenticate using the specified (or default) strategy.
   * Returns an Express middleware function.
   */
  authenticate(strategyName, options = {}) {
    const strategy = this.get(strategyName);
    return strategy.authenticate(options);
  }

  /**
   * Handle the SSO callback for the specified strategy.
   * Returns an Express middleware function.
   */
  callback(strategyName, options = {}) {
    const strategy = this.get(strategyName);
    return strategy.callback(options);
  }

  /**
   * Logout using the specified strategy.
   * Returns an Express middleware function.
   */
  logout(strategyName, options = {}) {
    const strategy = this.get(strategyName);
    if (typeof strategy.logout !== 'function') {
      return (req, res, next) => {
        req.session && req.session.destroy();
        next();
      };
    }
    return strategy.logout(options);
  }

  /**
   * Mount all strategies as sub-routes under a given Express router.
   *
   * Routes created per strategy named "foo":
   *   GET  /foo/login     — initiates the SSO flow
   *   GET  /foo/callback  — handles the IdP response
   *   GET  /foo/logout    — terminates the session
   */
  router() {
    const express = require('express');
    const router = express.Router();

    for (const [name] of this.strategies) {
      router.get(`/${name}/login`, this.authenticate(name));
      router.get(`/${name}/callback`, this.callback(name));
      router.post(`/${name}/callback`, this.callback(name)); // SAML uses POST
      router.get(`/${name}/logout`, this.logout(name));
    }

    return router;
  }
}

module.exports = SSOManager;
