#!/usr/bin/env bash
# One-shot deploy for a fresh Ubuntu 22.04/24.04 VM (x86_64 or ARM64).
# Installs Docker, opens port 80, clones the repo, and starts the full stack.
#
#   curl -fsSL https://raw.githubusercontent.com/jaiswalabhi1703/Vyapaar/main/deploy/setup-server.sh | bash
#
# Optional env before running: PAYU_KEY / PAYU_SALT (PayU test-merchant credentials).
set -euo pipefail

REPO_URL="https://github.com/jaiswalabhi1703/Vyapaar.git"
APP_DIR="$HOME/vyapaar"

echo "==> Installing Docker (skipped if already present)"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

echo "==> Opening port 80 (Oracle Ubuntu images ship restrictive iptables rules)"
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
if command -v netfilter-persistent >/dev/null 2>&1; then
  sudo netfilter-persistent save || true
fi

echo "==> Cloning/updating the app"
if [ -d "$APP_DIR/.git" ]; then
  git -C "$APP_DIR" pull --ff-only
else
  git clone "$REPO_URL" "$APP_DIR"
fi
cd "$APP_DIR"

PUBLIC_IP=$(curl -fsS https://api.ipify.org || hostname -I | awk '{print $1}')

echo "==> Writing .env (random JWT secret, app served on port 80)"
if [ ! -f .env ]; then
  cat > .env <<EOF
JWT_SECRET=$(head -c 48 /dev/urandom | base64 | tr -d '/+=')
FRONTEND_PORT=80
PAYU_KEY=${PAYU_KEY:-}
PAYU_SALT=${PAYU_SALT:-}
PAYU_SURL=http://$PUBLIC_IP/payments/payu/callback
PAYU_FURL=http://$PUBLIC_IP/payments/payu/callback
PAYU_RETURN_URL=http://$PUBLIC_IP
EOF
fi

echo "==> Building and starting the stack (first build takes ~10 min)"
sudo docker compose -f docker-compose.prod.yml up -d --build

echo ""
echo "==============================================="
echo "  Deployed. Open:  http://$PUBLIC_IP"
echo "  Login:           demo / password"
echo "==============================================="
