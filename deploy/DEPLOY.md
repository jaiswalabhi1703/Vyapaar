# Deploying Vyapaar for free

The stack (5 Spring Boot services + Kafka + Redis + React) needs ~4 GB RAM, which rules out
the small "no card" free tiers — Render, Railway, Vercel and Netlify cannot run Kafka for
free. The recommended home is a single free VM running `docker-compose.prod.yml`.

**Recommended: Oracle Cloud Always Free** — 4 ARM CPUs / 24 GB RAM, free *forever* (not a
trial). Signup requires a debit/credit card for identity verification only; nothing is
charged (a small temporary hold appears and disappears).

Fallbacks (trial credits, also card-verified):
- **Google Cloud** — $300 / 90 days. Machine type `e2-standard-2` (8 GB) is plenty.
- **AWS** — free-plan credits. Instance `t3.medium` (4 GB) is the minimum.

---

## Step 1 — Create the Oracle Cloud account (~10 min, you must do this)

1. Go to <https://signup.cloud.oracle.com> and sign up with your email.
2. Pick the **home region closest to you with capacity** (for India: Hyderabad or Mumbai).
   The home region cannot be changed later, and Always Free VMs live only there.
3. Complete card verification. Stay on the **Always Free** tier (do not upgrade).

## Step 2 — Create the VM (~5 min)

1. Console → **Compute → Instances → Create instance**.
2. Image: **Ubuntu 24.04** (aarch64). Shape: **Ampere → VM.Standard.A1.Flex**,
   **4 OCPUs / 24 GB RAM** (the Always Free maximum).
3. Networking: accept the default VCN ("Create new virtual cloud network"), ensure
   **Assign a public IPv4 address** is checked.
4. **Download the SSH private key** it generates (or paste your own public key). Create.
5. If you get **"Out of capacity"**: try 2 OCPUs/12 GB, a different availability domain,
   or retry later — A1 capacity frees up throughout the day.

## Step 3 — Open port 80 in the cloud firewall (~2 min)

Instance page → click its **subnet** → **Default Security List** → **Add Ingress Rule**:
- Source CIDR `0.0.0.0/0`, protocol TCP, destination port **80**.

## Step 4 — Deploy (one command)

SSH in (`ssh -i <downloaded-key> ubuntu@<PUBLIC_IP>`) and run:

```bash
curl -fsSL https://raw.githubusercontent.com/jaiswalabhi1703/Vyapaar/main/deploy/setup-server.sh | bash
```

~10 minutes later the app is live at **http://\<PUBLIC_IP\>** (login `demo` / `password`).
The script installs Docker, opens the OS firewall, clones this repo, generates a random
`JWT_SECRET`, and starts everything with `docker compose -f docker-compose.prod.yml up -d`.

## PayU (optional)

Real PayU test checkout needs merchant credentials from <https://onboarding.payu.in/>
(test mode). On the server, edit `~/vyapaar/.env`, set `PAYU_KEY` and `PAYU_SALT`, then
`sudo docker compose -f docker-compose.prod.yml up -d payment-service`. The callback URL is
already routed through nginx at `http://<PUBLIC_IP>/payments/payu/callback` — register that
origin with PayU. Without credentials the built-in mock gateway is used (orders under $1000
approve, above decline).

## Updating the live app

```bash
cd ~/vyapaar && git pull && sudo docker compose -f docker-compose.prod.yml up -d --build
```

## Free HTTPS + domain later (optional)

Point a free subdomain (e.g. DuckDNS) at the IP and put Caddy in front for automatic
Let's Encrypt TLS. Not required to be live.
