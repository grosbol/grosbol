#!/bin/bash
# ============================================
# LET'S ENCRYPT SSL SETUP SCRIPT
# ============================================
# This script installs certbot and gets a free
# SSL certificate from Let's Encrypt.
#
# USAGE:
#   sudo bash setup-ssl.sh yourdomain.com
#
# REQUIREMENTS:
#   - A server (VPS) with a public IP address
#   - A domain name pointing to that IP (A record)
#   - Port 80 open in your firewall
#   - Run as root (sudo)
# ============================================

set -e

# ---------- CHECK INPUTS ----------

DOMAIN=$1

if [ -z "$DOMAIN" ]; then
  echo ""
  echo "  ERROR: Please provide your domain name!"
  echo ""
  echo "  Usage:  sudo bash setup-ssl.sh yourdomain.com"
  echo ""
  exit 1
fi

# ---------- CHECK ROOT ----------

if [ "$EUID" -ne 0 ]; then
  echo ""
  echo "  ERROR: This script must be run as root (use sudo)"
  echo ""
  echo "  Usage:  sudo bash setup-ssl.sh $DOMAIN"
  echo ""
  exit 1
fi

echo ""
echo "============================================"
echo "  Setting up Let's Encrypt for: $DOMAIN"
echo "============================================"
echo ""

# ---------- STEP 1: INSTALL CERTBOT ----------

echo "[Step 1/4] Installing certbot..."

# Detect the package manager and install certbot
if command -v apt-get &> /dev/null; then
  # Debian / Ubuntu
  apt-get update -qq
  apt-get install -y -qq certbot
elif command -v dnf &> /dev/null; then
  # Fedora / RHEL 8+
  dnf install -y -q certbot
elif command -v yum &> /dev/null; then
  # CentOS / RHEL 7
  yum install -y -q certbot
elif command -v snap &> /dev/null; then
  # Snap (works on many distros)
  snap install --classic certbot
  ln -sf /snap/bin/certbot /usr/bin/certbot
else
  echo "  Could not detect package manager."
  echo "  Please install certbot manually:"
  echo "    https://certbot.eff.org/"
  exit 1
fi

echo "  Done!"
echo ""

# ---------- STEP 2: CREATE WEBROOT DIRECTORY ----------

echo "[Step 2/4] Creating webroot directory..."

WEBROOT="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$WEBROOT/.well-known/acme-challenge"

echo "  Webroot: $WEBROOT"
echo "  Done!"
echo ""

# ---------- STEP 3: GET THE CERTIFICATE ----------

echo "[Step 3/4] Requesting certificate from Let's Encrypt..."
echo ""
echo "  IMPORTANT: Make sure your Node.js server is running on port 80:"
echo "    node server.js"
echo ""
echo "  Press Enter to continue (or Ctrl+C to cancel)..."
read -r

certbot certonly \
  --webroot \
  --webroot-path "$WEBROOT" \
  --domain "$DOMAIN" \
  --non-interactive \
  --agree-tos \
  --register-unsafely-without-email

echo ""
echo "  Done!"
echo ""

# ---------- STEP 4: SET UP AUTO-RENEWAL ----------

echo "[Step 4/4] Setting up automatic certificate renewal..."

# Certbot usually sets up a cron job or systemd timer automatically.
# Let's make sure by adding a cron job if one doesn't exist.
if ! crontab -l 2>/dev/null | grep -q "certbot renew"; then
  (crontab -l 2>/dev/null; echo "0 3 * * * certbot renew --quiet && systemctl restart certificate-app 2>/dev/null || true") | crontab -
  echo "  Added daily renewal cron job (runs at 3 AM)"
else
  echo "  Renewal cron job already exists"
fi

echo "  Done!"
echo ""

# ---------- ALL DONE! ----------

echo "============================================"
echo "  SSL SETUP COMPLETE!"
echo "============================================"
echo ""
echo "  Your certificates are stored at:"
echo "    /etc/letsencrypt/live/$DOMAIN/"
echo ""
echo "  Now start your server with HTTPS:"
echo "    sudo node server.js --production --domain $DOMAIN"
echo ""
echo "  Your app will be available at:"
echo "    https://$DOMAIN"
echo ""
echo "  Certificates auto-renew every 60-90 days."
echo "============================================"
echo ""
