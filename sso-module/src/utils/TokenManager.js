'use strict';

const jwt = require('jsonwebtoken');
const crypto = require('crypto');

/**
 * TokenManager — helpers for issuing, verifying, and refreshing tokens.
 */
class TokenManager {
  constructor(secret, options = {}) {
    if (!secret) {
      throw new Error('TokenManager: a signing secret is required.');
    }
    this.secret = secret;
    this.defaultTTL = options.defaultTTL || '1h';
    this.refreshTTL = options.refreshTTL || '7d';
    this._refreshTokenStore = new Map(); // In production use Redis or a DB
  }

  /**
   * Issue an access token.
   */
  issueAccessToken(payload, options = {}) {
    return jwt.sign(payload, this.secret, {
      expiresIn: options.expiresIn || this.defaultTTL,
      ...options,
    });
  }

  /**
   * Issue a refresh token and persist its hash.
   */
  issueRefreshToken(userId, options = {}) {
    const raw = crypto.randomBytes(40).toString('hex');
    const hash = crypto.createHash('sha256').update(raw).digest('hex');
    const expiresAt = Date.now() + this._parseTTL(options.expiresIn || this.refreshTTL);

    this._refreshTokenStore.set(hash, { userId, expiresAt });
    return raw;
  }

  /**
   * Validate a refresh token and return the associated userId.
   */
  validateRefreshToken(raw) {
    const hash = crypto.createHash('sha256').update(raw).digest('hex');
    const record = this._refreshTokenStore.get(hash);

    if (!record) {
      throw new Error('Invalid refresh token.');
    }
    if (Date.now() > record.expiresAt) {
      this._refreshTokenStore.delete(hash);
      throw new Error('Refresh token expired.');
    }

    return record.userId;
  }

  /**
   * Revoke a refresh token.
   */
  revokeRefreshToken(raw) {
    const hash = crypto.createHash('sha256').update(raw).digest('hex');
    this._refreshTokenStore.delete(hash);
  }

  /**
   * Verify and decode an access token.
   */
  verifyAccessToken(token, options = {}) {
    return jwt.verify(token, this.secret, options);
  }

  /**
   * Decode without verifying (use only for non-security purposes).
   */
  decode(token) {
    return jwt.decode(token);
  }

  _parseTTL(ttl) {
    if (typeof ttl === 'number') return ttl * 1000;
    const units = { s: 1000, m: 60000, h: 3600000, d: 86400000 };
    const match = String(ttl).match(/^(\d+)([smhd])$/);
    if (!match) throw new Error(`Invalid TTL format: ${ttl}`);
    return parseInt(match[1], 10) * units[match[2]];
  }
}

module.exports = TokenManager;
