# Build the SPA, then serve the static files from Nginx, which also reverse-proxies
# /api and /auth to the gateway (same-origin -> no CORS in the containerized demo).
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Drop any dev .env so the production build uses relative URLs (same-origin), which Nginx
# proxies to the gateway. Avoids the SPA pointing at a host port.
RUN rm -f .env .env.local .env.development
RUN npm run build

FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
