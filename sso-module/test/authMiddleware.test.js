'use strict';

const { authMiddleware, requireSSO } = require('../src/middleware/authMiddleware');

function buildReq(user = null) {
  return {
    session: { user },
    headers: {},
    xhr: false,
    originalUrl: '/protected',
  };
}

function buildRes() {
  const res = {
    status: jest.fn().mockReturnThis(),
    json: jest.fn(),
    redirect: jest.fn(),
  };
  return res;
}

describe('authMiddleware', () => {
  test('sets req.user from session', () => {
    const req = buildReq({ id: 1, email: 'x@y.com' });
    const res = buildRes();
    const next = jest.fn();

    authMiddleware(req, res, next);

    expect(req.user).toEqual({ id: 1, email: 'x@y.com' });
    expect(req.isAuthenticated()).toBe(true);
    expect(next).toHaveBeenCalled();
  });

  test('sets req.user to null when no session user', () => {
    const req = buildReq(null);
    const res = buildRes();
    const next = jest.fn();

    authMiddleware(req, res, next);

    expect(req.user).toBeNull();
    expect(req.isAuthenticated()).toBe(false);
  });
});

describe('requireSSO', () => {
  test('calls next() when authenticated', () => {
    const req = buildReq({ id: 1 });
    req.isAuthenticated = () => true;
    const res = buildRes();
    const next = jest.fn();

    requireSSO()(req, res, next);
    expect(next).toHaveBeenCalled();
  });

  test('redirects unauthenticated browser requests', () => {
    const req = buildReq(null);
    req.isAuthenticated = () => false;
    const res = buildRes();
    const next = jest.fn();

    requireSSO({ loginPath: '/login' })(req, res, next);

    expect(res.redirect).toHaveBeenCalledWith('/login?returnTo=%2Fprotected');
    expect(next).not.toHaveBeenCalled();
  });

  test('returns 401 JSON for XHR requests', () => {
    const req = buildReq(null);
    req.isAuthenticated = () => false;
    req.xhr = true;
    const res = buildRes();
    const next = jest.fn();

    requireSSO()(req, res, next);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({ error: 'Authentication required.' });
  });
});
