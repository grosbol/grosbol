'use strict';

require('dotenv').config();
const express = require('express');
const cookieParser = require('cookie-parser');

const {
  SSOManager,
  OIDCStrategy,
  OAuthStrategy,
  JWTStrategy,
  SessionManager,
  authMiddleware,
  requireSSO,
} = require('../src');

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());
app.use(SessionManager.middleware({ secret: process.env.SESSION_SECRET || 'dev-secret' }));
app.use(authMiddleware);

// ── SSO Manager setup ────────────────────────────────────────────────────────

const sso = new SSOManager({ defaultStrategy: 'oidc' });

// OIDC (e.g. Google, Okta, Auth0)
sso.register(
  'oidc',
  new OIDCStrategy({
    issuerURL: process.env.OIDC_ISSUER_URL || 'https://accounts.google.com',
    clientId: process.env.OIDC_CLIENT_ID || 'your-client-id',
    clientSecret: process.env.OIDC_CLIENT_SECRET || 'your-client-secret',
    callbackURL: process.env.OIDC_CALLBACK_URL || 'http://localhost:3000/auth/oidc/callback',
    onSuccess(req, res, userInfo) {
      console.log('OIDC login:', userInfo.email);
      const returnTo = req.session.returnTo || '/dashboard';
      delete req.session.returnTo;
      res.redirect(returnTo);
    },
    onFailure(req, res, err) {
      console.error('OIDC failure:', err.message);
      res.status(401).send('Authentication failed.');
    },
  })
);

// Generic OAuth2 (e.g. GitHub)
sso.register(
  'github',
  new OAuthStrategy({
    clientId: process.env.GITHUB_CLIENT_ID || 'your-github-client-id',
    clientSecret: process.env.GITHUB_CLIENT_SECRET || 'your-github-client-secret',
    authorizationURL: 'https://github.com/login/oauth/authorize',
    tokenURL: 'https://github.com/login/oauth/access_token',
    userInfoURL: 'https://api.github.com/user',
    callbackURL: process.env.GITHUB_CALLBACK_URL || 'http://localhost:3000/auth/github/callback',
    scope: ['read:user', 'user:email'],
    onSuccess(req, res, user) {
      console.log('GitHub login:', user.login);
      res.redirect('/dashboard');
    },
  })
);

// JWT strategy for API routes
const jwtStrategy = new JWTStrategy({
  secret: process.env.JWT_SECRET || 'super-secret',
  algorithms: ['HS256'],
  issuer: 'my-app',
});

// ── Mount SSO routes ─────────────────────────────────────────────────────────

app.use('/auth', sso.router());

// ── Application routes ───────────────────────────────────────────────────────

app.get('/', (req, res) => {
  if (req.isAuthenticated()) {
    return res.redirect('/dashboard');
  }
  res.send(`
    <h1>SSO Demo</h1>
    <a href="/auth/oidc/login">Login with OIDC</a><br>
    <a href="/auth/github/login">Login with GitHub</a>
  `);
});

app.get('/dashboard', requireSSO({ loginPath: '/' }), (req, res) => {
  res.json({ message: 'Welcome!', user: req.user });
});

// Protected API endpoint using JWT
app.get('/api/me', jwtStrategy.authenticate(), (req, res) => {
  res.json({ user: req.user });
});

// Issue a JWT (demo — in production gate behind a real login)
app.post('/api/token', (req, res) => {
  const { userId, email } = req.body;
  if (!userId) return res.status(400).json({ error: 'userId required' });
  const token = jwtStrategy.sign({ sub: userId, email }, { expiresIn: '2h' });
  res.json({ token });
});

// ── Start ────────────────────────────────────────────────────────────────────

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`SSO demo server running on http://localhost:${PORT}`);
});
