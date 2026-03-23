'use strict';

const SSOManager = require('../src/SSOManager');

function mockStrategy(name) {
  return {
    name,
    authenticate: () => (req, res, next) => next(),
    callback: () => (req, res, next) => next(),
  };
}

describe('SSOManager', () => {
  let manager;

  beforeEach(() => {
    manager = new SSOManager();
  });

  test('registers and lists strategies', () => {
    manager.register('oidc', mockStrategy('oidc'));
    manager.register('saml', mockStrategy('saml'));
    expect(manager.list()).toEqual(['oidc', 'saml']);
  });

  test('throws on duplicate registration', () => {
    manager.register('oidc', mockStrategy('oidc'));
    expect(() => manager.register('oidc', mockStrategy('oidc'))).toThrow('already registered');
  });

  test('unregisters a strategy', () => {
    manager.register('oidc', mockStrategy('oidc'));
    manager.unregister('oidc');
    expect(manager.list()).toEqual([]);
  });

  test('throws when getting an unknown strategy', () => {
    expect(() => manager.get('unknown')).toThrow('not found');
  });

  test('emits "strategy:registered" event', () => {
    const listener = jest.fn();
    manager.on('strategy:registered', listener);
    manager.register('jwt', mockStrategy('jwt'));
    expect(listener).toHaveBeenCalledWith(expect.objectContaining({ name: 'jwt' }));
  });

  test('authenticate returns a middleware function', () => {
    manager.register('oidc', mockStrategy('oidc'));
    const mw = manager.authenticate('oidc');
    expect(typeof mw).toBe('function');
  });
});
