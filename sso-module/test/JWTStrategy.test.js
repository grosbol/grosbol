'use strict';

const JWTStrategy = require('../src/strategies/JWTStrategy');

const SECRET = 'test-secret-for-jest-tests-only';

function buildReq(authHeader) {
  return {
    headers: authHeader ? { authorization: authHeader } : {},
    cookies: {},
  };
}

function buildRes() {
  return { status: jest.fn().mockReturnThis(), json: jest.fn() };
}

describe('JWTStrategy', () => {
  let strategy;

  beforeEach(() => {
    strategy = new JWTStrategy({ secret: SECRET, issuer: 'test-app' });
  });

  test('signs and verifies a token', () => {
    const token = strategy.sign({ sub: 'u1' }, { expiresIn: '1h' });
    expect(typeof token).toBe('string');
    const payload = strategy.tokenManager.verifyAccessToken(token, { issuer: 'test-app' });
    expect(payload.sub).toBe('u1');
  });

  test('authenticate sets req.user on valid token', (done) => {
    const token = strategy.sign({ sub: 'u2', role: 'admin' });
    const req = buildReq(`Bearer ${token}`);
    req.user = null;
    const res = buildRes();

    strategy.authenticate()(req, res, (err) => {
      expect(err).toBeUndefined();
      expect(req.user.sub).toBe('u2');
      expect(req.user.role).toBe('admin');
      done();
    });
  });

  test('authenticate calls next(err) when no token provided', (done) => {
    const req = buildReq(null);
    const res = buildRes();

    strategy.authenticate()(req, res, (err) => {
      expect(err).toBeDefined();
      expect(err.status).toBe(401);
      done();
    });
  });

  test('authenticate calls next(err) on tampered token', (done) => {
    const token = strategy.sign({ sub: 'u3' }) + 'tampered';
    const req = buildReq(`Bearer ${token}`);
    const res = buildRes();

    strategy.authenticate()(req, res, (err) => {
      expect(err).toBeDefined();
      expect(err.status).toBe(401);
      done();
    });
  });

  test('throws on missing secret', () => {
    expect(() => new JWTStrategy({})).toThrow('"secret" or "publicKey" is required');
  });
});
