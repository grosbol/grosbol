# Certificate Generator

A beginner-friendly app that lets you create and download beautiful certificates as PDFs — served over HTTPS with free SSL from Let's Encrypt.

## Quick Start (Development)

```bash
# 1. Install dependencies
npm install

# 2. Start the server
npm start

# 3. Open in your browser
#    http://localhost:3000
```

Or just open `index.html` directly in your browser (no server needed for basic use).

## Deploy with HTTPS (Let's Encrypt)

### Prerequisites

- A server (VPS) — e.g. DigitalOcean, AWS EC2, Linode
- A domain name (e.g. `certificates.example.com`)
- Domain's DNS **A record** pointing to your server's IP

### Step-by-step

```bash
# 1. SSH into your server
ssh root@your-server-ip

# 2. Clone this repo
git clone https://github.com/grosbol/grosbol.git
cd grosbol

# 3. Install Node.js (if not installed)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo bash -
sudo apt-get install -y nodejs

# 4. Install dependencies
npm install

# 5. Start HTTP server (needed for certificate verification)
node server.js &

# 6. Run the SSL setup script
sudo bash setup-ssl.sh yourdomain.com

# 7. Restart with HTTPS
sudo node server.js --production --domain yourdomain.com
```

Your app is now live at `https://yourdomain.com` with a free SSL certificate!

### Certificate Renewal

Let's Encrypt certificates expire every 90 days. The setup script adds a cron job that auto-renews them — no action needed.

To manually renew:

```bash
sudo certbot renew
```

## How It Works

| File | What it does |
|---|---|
| `index.html` | Page structure — form + certificate preview |
| `style.css` | Styling — gold-bordered certificate, responsive layout |
| `app.js` | Live preview + PDF download (html2canvas + jsPDF) |
| `server.js` | Express server with HTTP/HTTPS support |
| `setup-ssl.sh` | Installs certbot and gets Let's Encrypt certificates |

## Project Structure

```
certificate-generator/
├── index.html          # Main page
├── style.css           # Styles
├── app.js              # Client-side JavaScript
├── server.js           # Node.js server (HTTP + HTTPS)
├── setup-ssl.sh        # Let's Encrypt setup script
├── package.json        # Dependencies
└── README.md           # You are here
```
