'use strict';

const SessionManager = require('../utils/SessionManager');

/**
 * authMiddleware — attaches session user to req.user for every request.
 */
function authMiddleware(req, res, next) {
  req.user = SessionManager.getUser(req) || null;
  req.isAuthenticated = () => !!req.user;
  next();
}

/**
 * requireSSO — route guard that redirects unauthenticated requests.
 *
 * Options:
 *   loginPath   {string}  Where to redirect (default: '/auth/login')
 *   returnTo    {boolean} Whether to pass returnTo query param (default: true)
 */
function requireSSO(options = {}) {
  const loginPath = options.loginPath || '/auth/login';
  const useReturnTo = options.returnTo !== false;

  return (req, res, next) => {
    if (SessionManager.isAuthenticated(req)) {
      return next();
    }

    if (req.xhr || (req.headers.accept && req.headers.accept.includes('application/json'))) {
      return res.status(401).json({ error: 'Authentication required.' });
    }

    const redirect = useReturnTo
      ? `${loginPath}?returnTo=${encodeURIComponent(req.originalUrl)}`
      : loginPath;

    res.redirect(redirect);
  };
}

module.exports = { authMiddleware, requireSSO };
