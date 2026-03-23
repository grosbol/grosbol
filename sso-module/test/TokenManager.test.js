'use strict';

const TokenManager = require('../src/utils/TokenManager');

describe('TokenManager', () => {
  let tm;

  beforeEach(() => {
    tm = new TokenManager('test-secret-32-chars-long-minimum!');
  });

  test('issues and verifies access tokens', () => {
    const token = tm.issueAccessToken({ sub: 'user1', email: 'a@b.com' });
    const payload = tm.verifyAccessToken(token);
    expect(payload.sub).toBe('user1');
    expect(payload.email).toBe('a@b.com');
  });

  test('refresh token round-trip', () => {
    const raw = tm.issueRefreshToken('user42');
    const userId = tm.validateRefreshToken(raw);
    expect(userId).toBe('user42');
  });

  test('revoked refresh token throws', () => {
    const raw = tm.issueRefreshToken('user42');
    tm.revokeRefreshToken(raw);
    expect(() => tm.validateRefreshToken(raw)).toThrow('Invalid refresh token');
  });

  test('throws with invalid/unknown refresh token', () => {
    expect(() => tm.validateRefreshToken('not-a-real-token')).toThrow('Invalid refresh token');
  });

  test('throws on missing secret', () => {
    expect(() => new TokenManager('')).toThrow('signing secret is required');
  });
});
