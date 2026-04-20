'use strict';

const SSOManager = require('./SSOManager');
const OAuthStrategy = require('./strategies/OAuthStrategy');
const OIDCStrategy = require('./strategies/OIDCStrategy');
const SAMLStrategy = require('./strategies/SAMLStrategy');
const JWTStrategy = require('./strategies/JWTStrategy');
const { authMiddleware, requireSSO } = require('./middleware/authMiddleware');
const TokenManager = require('./utils/TokenManager');
const SessionManager = require('./utils/SessionManager');

module.exports = {
  SSOManager,
  OAuthStrategy,
  OIDCStrategy,
  SAMLStrategy,
  JWTStrategy,
  authMiddleware,
  requireSSO,
  TokenManager,
  SessionManager,
};
