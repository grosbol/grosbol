# node-sso-module

A flexible Single Sign-On (SSO) module for Node.js that supports:

| Strategy | Protocol | Use case |
|----------|----------|----------|
| `OIDCStrategy` | OpenID Connect 1.0 | Google, Auth0, Okta, Azure AD |
| `OAuthStrategy` | OAuth 2.0 Auth Code | GitHub, any OAuth2 provider |
| `SAMLStrategy` | SAML 2.0 | Enterprise IdPs (Okta, ADFS, OneLogin) |
| `JWTStrategy` | JWT Bearer | Stateless API authentication |

---

## Installation

```bash
npm install
```

Copy `.env.example` to `.env` and fill in your IdP credentials.

---

## Quick Start

```js
const express = require('express');
const { SSOManager, OIDCStrategy, SessionManager, authMiddleware, requireSSO } = require('./src');

const app = express();
app.use(SessionManager.middleware({ secret: process.env.SESSION_SECRET }));
app.use(authMiddleware);

const sso = new SSOManager();

sso.register('oidc', new OIDCStrategy({
  issuerURL:    process.env.OIDC_ISSUER_URL,
  clientId:     process.env.OIDC_CLIENT_ID,
  clientSecret: process.env.OIDC_CLIENT_SECRET,
  callbackURL:  process.env.OIDC_CALLBACK_URL,
  onSuccess(req, res) { res.redirect('/dashboard'); },
}));

app.use('/auth', sso.router());

// Protect routes
app.get('/dashboard', requireSSO(), (req, res) => {
  res.json({ user: req.user });
});

app.listen(3000);
```

This mounts three routes automatically per strategy:

| Route | Description |
|-------|-------------|
| `GET /auth/oidc/login` | Initiates the SSO flow |
| `GET /auth/oidc/callback` | Handles the IdP redirect |
| `GET /auth/oidc/logout` | Ends the session |

---

## Strategies

### OIDCStrategy

Performs full OpenID Connect discovery and PKCE automatically.

```js
new OIDCStrategy({
  issuerURL:    'https://accounts.google.com',
  clientId:     '...',
  clientSecret: '...',
  callbackURL:  'http://localhost:3000/auth/oidc/callback',
  scope:        ['openid', 'profile', 'email'], // default
  onSuccess(req, res, userInfo, tokenSet) { ... },
  onFailure(req, res, err) { ... },
})
```

### OAuthStrategy

Generic OAuth 2.0 Authorization Code flow.

```js
new OAuthStrategy({
  clientId:         '...',
  clientSecret:     '...',
  authorizationURL: 'https://github.com/login/oauth/authorize',
  tokenURL:         'https://github.com/login/oauth/access_token',
  userInfoURL:      'https://api.github.com/user',
  callbackURL:      'http://localhost:3000/auth/github/callback',
  scope:            ['read:user', 'user:email'],
  onSuccess(req, res, user, tokens) { ... },
})
```

### SAMLStrategy

SAML 2.0 Service Provider. Handles HTTP-Redirect (AuthnRequest) and HTTP-POST (Response).

```js
new SAMLStrategy({
  sp: {
    entityID: 'http://localhost:3000',
    assertionConsumerService: [{
      binding:  'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST',
      location: 'http://localhost:3000/auth/saml/callback',
    }],
    privateKey:  './certs/sp-private.pem',
    certificate: './certs/sp-cert.pem',
  },
  idp: {
    metadataPath: './idp-metadata.xml', // or configure manually
  },
  onSuccess(req, res, user) { ... },
})
```

Expose SP metadata at `/auth/saml/metadata` by registering it manually:

```js
app.get('/auth/saml/metadata', sso.get('saml').metadata());
```

### JWTStrategy

Stateless Bearer token verification for APIs.

```js
const jwt = new JWTStrategy({
  secret:     process.env.JWT_SECRET,
  algorithms: ['HS256'],
  issuer:     'my-app',
});

// Issue a token
app.post('/login', (req, res) => {
  const token = jwt.sign({ sub: req.body.userId });
  res.json({ token });
});

// Protect routes
app.get('/api/me', jwt.authenticate(), (req, res) => {
  res.json({ user: req.user });
});
```

---

## Middleware

### `authMiddleware`

Populates `req.user` and `req.isAuthenticated()` from the session on every request.

```js
app.use(authMiddleware);
```

### `requireSSO(options)`

Route guard. Redirects unauthenticated browser requests; returns `401 JSON` for XHR/API calls.

```js
app.get('/secret', requireSSO({ loginPath: '/auth/oidc/login' }), handler);
```

Options:

| Option | Default | Description |
|--------|---------|-------------|
| `loginPath` | `/auth/login` | Where to redirect |
| `returnTo` | `true` | Append `?returnTo=<url>` to the redirect |

---

## Running Tests

```bash
npm test
```

---

## Running the Example Server

```bash
cp .env.example .env   # fill in your credentials
npm start
```
