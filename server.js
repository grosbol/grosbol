// ===========================
// CERTIFICATE APP — SERVER
// ===========================
// This server does three things:
//   1. Serves the static certificate app files
//   2. Uses HTTPS with Let's Encrypt certificates (if available)
//   3. Redirects HTTP traffic to HTTPS automatically
//
// HOW TO RUN:
//   Development (no SSL):  node server.js
//   Production (with SSL): sudo node server.js --production
//   Custom domain:         sudo node server.js --production --domain example.com

const express = require("express");
const path = require("path");
const fs = require("fs");
const http = require("http");
const https = require("https");

// ---------- PARSE COMMAND-LINE FLAGS ----------

const args = process.argv.slice(2);
const isProduction = args.includes("--production");
const domainIndex = args.indexOf("--domain");
const domain = domainIndex !== -1 ? args[domainIndex + 1] : "localhost";

// ---------- PORTS ----------

const HTTP_PORT = isProduction ? 80 : 3000;
const HTTPS_PORT = 443;

// ---------- CREATE THE APP ----------

const app = express();

// Serve all static files (index.html, style.css, app.js)
app.use(express.static(path.join(__dirname, "public")));

// Also serve from root directory for backward compatibility
app.use(express.static(__dirname));

// Let's Encrypt needs this folder to verify you own the domain
// Certbot places challenge files here during certificate issuance
app.use(
  "/.well-known/acme-challenge",
  express.static(path.join(__dirname, ".well-known", "acme-challenge"))
);

// ---------- START THE SERVER ----------

if (isProduction) {
  // --- PRODUCTION MODE: HTTPS with Let's Encrypt ---

  // Path where Let's Encrypt stores certificates
  const certPath = `/etc/letsencrypt/live/${domain}`;

  // Check if certificates exist
  if (
    fs.existsSync(path.join(certPath, "fullchain.pem")) &&
    fs.existsSync(path.join(certPath, "privkey.pem"))
  ) {
    // Read the SSL certificate files
    const sslOptions = {
      cert: fs.readFileSync(path.join(certPath, "fullchain.pem")),
      key: fs.readFileSync(path.join(certPath, "privkey.pem")),
    };

    // Start HTTPS server (the secure one)
    https.createServer(sslOptions, app).listen(HTTPS_PORT, function () {
      console.log("---------------------------------------------");
      console.log("  HTTPS server running!");
      console.log("  Open: https://" + domain);
      console.log("---------------------------------------------");
    });

    // Start HTTP server that redirects everything to HTTPS
    const redirectApp = express();

    // Keep the ACME challenge route for certificate renewals
    redirectApp.use(
      "/.well-known/acme-challenge",
      express.static(path.join(__dirname, ".well-known", "acme-challenge"))
    );

    // Redirect all other HTTP requests to HTTPS
    redirectApp.get("*", function (req, res) {
      res.redirect("https://" + req.headers.host + req.url);
    });

    http.createServer(redirectApp).listen(HTTP_PORT, function () {
      console.log("  HTTP -> HTTPS redirect active on port " + HTTP_PORT);
    });
  } else {
    // Certificates don't exist yet — run HTTP only so certbot can verify
    console.log("---------------------------------------------");
    console.log("  SSL certificates not found!");
    console.log("  Starting HTTP-only server for certificate setup...");
    console.log("");
    console.log("  Run this to get your certificates:");
    console.log("    sudo bash setup-ssl.sh " + domain);
    console.log("");
    console.log("  Then restart with:");
    console.log("    sudo node server.js --production --domain " + domain);
    console.log("---------------------------------------------");

    http.createServer(app).listen(HTTP_PORT, function () {
      console.log("  HTTP server running on port " + HTTP_PORT);
    });
  }
} else {
  // --- DEVELOPMENT MODE: HTTP only ---

  http.createServer(app).listen(HTTP_PORT, function () {
    console.log("---------------------------------------------");
    console.log("  Development server running!");
    console.log("  Open: http://localhost:" + HTTP_PORT);
    console.log("---------------------------------------------");
    console.log("  To use HTTPS with Let's Encrypt:");
    console.log("  1. Get a domain name pointing to your server");
    console.log("  2. Run: sudo bash setup-ssl.sh yourdomain.com");
    console.log("  3. Run: sudo node server.js --production --domain yourdomain.com");
    console.log("---------------------------------------------");
  });
}
